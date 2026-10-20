package ke.shulehub.payments;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ke.shulehub.payments.internal.Daraja;
import ke.shulehub.payments.internal.DarajaClient;
import ke.shulehub.payments.internal.MpesaChannel;
import ke.shulehub.payments.internal.MpesaChannelRepository;
import ke.shulehub.payments.internal.PaymentIntent;
import ke.shulehub.payments.internal.PaymentIntentRepository;
import ke.shulehub.shared.contact.PhoneNumbers;
import ke.shulehub.shared.money.Money;
import ke.shulehub.shared.web.BadRequestException;
import ke.shulehub.shared.web.ConflictException;
import ke.shulehub.shared.web.GatewayException;
import ke.shulehub.shared.web.NotFoundException;

/**
 * STK Push ("Lipa na M-Pesa Online"): sends a payment prompt to a parent's phone. The intent
 * is saved before Safaricom is called and updated after, and no database transaction is held
 * open while waiting on the network.
 */
@Service
public class MpesaGateway {

    public record StkRequest(UUID branchId, UUID learnerId, UUID invoiceId, BigDecimal amount, String phone,
            String accountReference, UUID requestedByPersonId, String idempotencyKey) {
    }

    public record StkInitiation(UUID intentId, PaymentIntentStatus status, String customerMessage) {
    }

    public record StkStatus(UUID intentId, UUID learnerId, PaymentIntentStatus status, BigDecimal amount, String phone,
            String resultDesc, String mpesaReceipt, Instant updatedAt) {
    }

    private static final Logger log = LoggerFactory.getLogger(MpesaGateway.class);
    private static final Duration QUERY_AFTER = Duration.ofSeconds(45);

    private final MpesaChannelRepository channels;
    private final PaymentIntentRepository intents;
    private final MpesaChannels channelConfig;
    private final DarajaClient daraja;
    private final Clock clock;
    private final TransactionTemplate transactions;

    public MpesaGateway(MpesaChannelRepository channels, PaymentIntentRepository intents, MpesaChannels channelConfig,
            DarajaClient daraja, Clock clock, PlatformTransactionManager transactionManager) {
        this.channels = channels;
        this.intents = intents;
        this.channelConfig = channelConfig;
        this.daraja = daraja;
        this.clock = clock;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /** Callers authorise first (bursar for the branch, or the learner's guardian). */
    public StkInitiation initiateStk(StkRequest request) {
        if (!Money.isPositive(request.amount()) || !Money.isWholeShillings(request.amount())) {
            throw new BadRequestException("M-Pesa amounts must be whole shillings of at least KES 1");
        }
        String phone = PhoneNumbers.require(request.phone(), "Phone number");
        String accountReference = request.accountReference().length() > 12
                ? request.accountReference().substring(0, 12) : request.accountReference();

        record Prepared(PaymentIntent intent, MpesaChannel channel, boolean existing) {
        }
        Prepared prepared = transactions.execute(status -> {
            var existing = intents.findByIdempotencyKey(request.idempotencyKey());
            MpesaChannel channel = channels.findFirstByBranchIdAndActiveTrueOrderByCreatedAtDesc(request.branchId())
                    .orElseThrow(() -> new ConflictException("M-Pesa is not set up for this branch yet"));
            if (existing.isPresent()) {
                return new Prepared(existing.get(), channel, true);
            }
            PaymentIntent intent = intents.save(new PaymentIntent(request.branchId(), channel.getId(),
                    request.learnerId(), request.invoiceId(), Money.of(request.amount()), phone, accountReference,
                    request.idempotencyKey(), request.requestedByPersonId()));
            return new Prepared(intent, channel, false);
        });
        if (prepared.existing()) {
            return new StkInitiation(prepared.intent().getId(), prepared.intent().getStatus(),
                    "This payment request was already sent");
        }

        MpesaChannel channel = prepared.channel();
        String timestamp = daraja.timestamp();
        Daraja.StkPushRequest push = new Daraja.StkPushRequest(channel.getShortcode(),
                DarajaClient.password(channel.getShortcode(), channelConfig.passkey(channel), timestamp), timestamp,
                channel.getChannelType() == MpesaChannelType.TILL ? "CustomerBuyGoodsOnline"
                        : "CustomerPayBillOnline",
                request.amount().longValueExact(), phone, channel.receivingParty(), phone,
                channelConfig.urls(channel).stk(), accountReference, "School fees");
        UUID intentId = prepared.intent().getId();
        try {
            Daraja.StkPushResponse response = daraja.stkPush(channelConfig.credentials(channel), push);
            boolean accepted = "0".equals(response.responseCode());
            transactions.executeWithoutResult(status -> {
                PaymentIntent intent = intents.findById(intentId).orElseThrow();
                if (accepted) {
                    intent.submitted(response.merchantRequestId(), response.checkoutRequestId(),
                            response.responseDescription());
                }
                else {
                    intent.failed(response.responseCode(), response.responseDescription(), clock.instant());
                }
            });
            return new StkInitiation(intentId,
                    accepted ? PaymentIntentStatus.SUBMITTED : PaymentIntentStatus.FAILED,
                    accepted ? "Check your phone and enter your M-Pesa PIN to pay"
                            : "M-Pesa could not send the prompt: " + response.responseDescription());
        }
        catch (GatewayException ex) {
            transactions.executeWithoutResult(status -> intents.findById(intentId)
                    .ifPresent(intent -> intent.failed("GATEWAY", ex.getMessage(), clock.instant())));
            throw ex;
        }
    }

    /**
     * Current state of an intent. If Safaricom's callback is late, asks Daraja once the prompt is
     * old enough; a failed or cancelled prompt is marked failed. A success still waits for the
     * callback, which carries the receipt number.
     */
    public StkStatus status(UUID intentId) {
        PaymentIntent intent = transactions.execute(status -> intents.findById(intentId)
                .orElseThrow(() -> new NotFoundException("Payment request not found")));
        if (intent.getStatus() == PaymentIntentStatus.SUBMITTED && intent.getCheckoutRequestId() != null
                && intent.getUpdatedAt().plus(QUERY_AFTER).isBefore(clock.instant())) {
            refreshFromDaraja(intent);
        }
        PaymentIntent current = transactions.execute(status -> intents.findById(intentId).orElseThrow());
        return new StkStatus(current.getId(), current.getLearnerId(), current.getStatus(), current.getAmount(),
                current.getPhone(), current.getResultDesc(), current.getMpesaReceipt(), current.getUpdatedAt());
    }

    private void refreshFromDaraja(PaymentIntent intent) {
        try {
            MpesaChannel channel = transactions.execute(status -> channels.findById(intent.getChannelId()).orElseThrow());
            String timestamp = daraja.timestamp();
            Daraja.StkQueryResponse response = daraja.stkQuery(channelConfig.credentials(channel),
                    new Daraja.StkQueryRequest(channel.getShortcode(),
                            DarajaClient.password(channel.getShortcode(), channelConfig.passkey(channel), timestamp),
                            timestamp, intent.getCheckoutRequestId()));
            if (response.resultCode() != null && !"0".equals(response.resultCode())) {
                transactions.executeWithoutResult(status -> intents.findById(intent.getId())
                        .filter(found -> !found.isFinished())
                        .ifPresent(found -> found.failed(response.resultCode(), response.resultDesc(), clock.instant())));
            }
        }
        catch (GatewayException ex) {
            log.info("STK query for {} failed, will rely on the callback: {}", intent.getId(), ex.getMessage());
        }
    }
}
