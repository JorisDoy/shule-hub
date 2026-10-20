package ke.shulehub.payments;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.json.JsonMapper;

import ke.shulehub.learners.LearnerDirectory;
import ke.shulehub.learners.LearnerSummary;
import ke.shulehub.payments.internal.CallbackLog;
import ke.shulehub.payments.internal.Daraja;
import ke.shulehub.payments.internal.MpesaChannel;
import ke.shulehub.payments.internal.MpesaChannelRepository;
import ke.shulehub.payments.internal.PaymentIntent;
import ke.shulehub.payments.internal.PaymentIntentRepository;
import ke.shulehub.shared.config.ShuleHubProperties;
import ke.shulehub.shared.config.TimeConfig;
import ke.shulehub.shared.money.Money;
import ke.shulehub.shared.tenancy.TenantContext;
import ke.shulehub.shared.web.BadRequestException;
import ke.shulehub.shared.web.NotFoundException;

/**
 * Handles Safaricom's callbacks. Each is logged raw, matched to a channel by the secret token
 * in its URL, then processed as that channel's tenant. Repeats are harmless: intents finish once
 * and the fees ledger de-duplicates on the M-Pesa receipt number.
 */
@Service
public class MobileMoneyCallbacks {

    private record ChannelRef(UUID id, UUID tenantId, UUID branchId) {
    }

    private static final Logger log = LoggerFactory.getLogger(MobileMoneyCallbacks.class);
    private static final DateTimeFormatter TRANS_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MpesaChannelRepository channels;
    private final PaymentIntentRepository intents;
    private final LearnerDirectory learners;
    private final CallbackLog callbackLog;
    private final ApplicationEventPublisher events;
    private final JsonMapper json;
    private final ShuleHubProperties properties;
    private final Clock clock;
    private final TransactionTemplate transactions;

    public MobileMoneyCallbacks(MpesaChannelRepository channels, PaymentIntentRepository intents,
            LearnerDirectory learners, CallbackLog callbackLog, ApplicationEventPublisher events, JsonMapper json,
            ShuleHubProperties properties, Clock clock, PlatformTransactionManager transactionManager) {
        this.channels = channels;
        this.intents = intents;
        this.learners = learners;
        this.callbackLog = callbackLog;
        this.events = events;
        this.json = json;
        this.properties = properties;
        this.clock = clock;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public Daraja.CallbackAck stkResult(String callbackToken, String rawBody) {
        UUID logId = callbackLog.record("STK", callbackToken, rawBody);
        ChannelRef channel = channel(callbackToken);
        Daraja.StkCallbackEnvelope envelope = parse(rawBody, Daraja.StkCallbackEnvelope.class);
        Daraja.StkCallback callback = envelope.body() == null ? null : envelope.body().stkCallback();
        if (callback == null || callback.checkoutRequestId() == null) {
            throw new BadRequestException("Not an STK callback");
        }
        String outcome = TenantContext.callAs(channel.tenantId(), () -> transactions.execute(status -> {
            Optional<PaymentIntent> found = intents.findByCheckoutRequestId(callback.checkoutRequestId());
            if (found.isEmpty()) {
                return "UNKNOWN_CHECKOUT";
            }
            PaymentIntent intent = found.get();
            if (intent.isFinished()) {
                return "DUPLICATE";
            }
            Instant now = clock.instant();
            if (callback.resultCode() == null || callback.resultCode() != 0) {
                intent.failed(String.valueOf(callback.resultCode()), callback.resultDesc(), now);
                return "FAILED_" + callback.resultCode();
            }
            String receipt = Optional.ofNullable(callback.item("MpesaReceiptNumber"))
                    .orElse(callback.checkoutRequestId());
            String amountText = callback.item("Amount");
            BigDecimal amount = amountText == null ? intent.getAmount() : Money.of(amountText);
            String phone = Optional.ofNullable(callback.item("PhoneNumber")).orElse(intent.getPhone());
            Instant paidAt = parseTime(callback.item("TransactionDate")).orElse(now);
            intent.succeeded(receipt, callback.resultDesc(), now);
            events.publishEvent(new MpesaPaymentReceived(channel.tenantId(), channel.branchId(),
                    MpesaPaymentReceived.Source.STK, receipt, amount, phone, null, intent.getAccountReference(),
                    intent.getLearnerId(), intent.getInvoiceId(), intent.getId(), paidAt, rawBody));
            return "PAID";
        }));
        callbackLog.outcome(logId, channel.tenantId(), outcome);
        return Daraja.CallbackAck.ok();
    }

    /** Safaricom asks before completing a paybill payment; we refuse unknown account numbers. */
    public Daraja.C2bResponse validateC2b(String callbackToken, String rawBody) {
        UUID logId = callbackLog.record("C2B_VALIDATION", callbackToken, rawBody);
        ChannelRef channel = channel(callbackToken);
        Daraja.C2bPayload payload = parse(rawBody, Daraja.C2bPayload.class);
        boolean known = TenantContext.callAs(channel.tenantId(),
                () -> learnerInBranch(payload.billRefNumber(), channel.branchId()).isPresent());
        boolean accept = known || !properties.mpesa().rejectUnknownAccounts();
        callbackLog.outcome(logId, channel.tenantId(), accept ? "ACCEPTED" : "REJECTED_UNKNOWN_ACCOUNT");
        return accept ? Daraja.C2bResponse.accept() : Daraja.C2bResponse.rejectInvalidAccount();
    }

    /** A completed paybill/till payment. Unmatched account numbers land in the bursar's suspense list. */
    public Daraja.C2bResponse confirmC2b(String callbackToken, String rawBody) {
        UUID logId = callbackLog.record("C2B_CONFIRMATION", callbackToken, rawBody);
        ChannelRef channel = channel(callbackToken);
        Daraja.C2bPayload payload = parse(rawBody, Daraja.C2bPayload.class);
        if (payload.transId() == null || payload.transAmount() == null) {
            throw new BadRequestException("Not a C2B confirmation");
        }
        String outcome = TenantContext.callAs(channel.tenantId(), () -> transactions.execute(status -> {
            Optional<LearnerSummary> learner = learnerInBranch(payload.billRefNumber(), channel.branchId());
            events.publishEvent(new MpesaPaymentReceived(channel.tenantId(), channel.branchId(),
                    MpesaPaymentReceived.Source.C2B, payload.transId(), Money.of(payload.transAmount()),
                    payload.msisdn(), payload.payerName(), payload.billRefNumber(),
                    learner.map(LearnerSummary::id).orElse(null), null, null,
                    parseTime(payload.transTime()).orElse(clock.instant()), rawBody));
            return learner.isPresent() ? "RECEIVED" : "RECEIVED_UNMATCHED";
        }));
        callbackLog.outcome(logId, channel.tenantId(), outcome);
        return new Daraja.C2bResponse("0", "Success");
    }

    /** A learner matches only within the branch that owns the paybill; anything else goes to suspense. */
    private Optional<LearnerSummary> learnerInBranch(String accountReference, UUID branchId) {
        return learners.findByAdmissionNumber(accountReference)
                .filter(learner -> learner.branchId().equals(branchId));
    }

    private ChannelRef channel(String callbackToken) {
        MpesaChannel channel = TenantContext.callAsRoot(() -> channels.findByCallbackToken(callbackToken))
                .orElseThrow(() -> new NotFoundException("Unknown callback"));
        return new ChannelRef(channel.getId(), channel.getTenantId(), channel.getBranchId());
    }

    private <T> T parse(String rawBody, Class<T> type) {
        try {
            T value = json.readValue(rawBody, type);
            if (value == null) {
                throw new BadRequestException("Empty callback");
            }
            return value;
        }
        catch (RuntimeException ex) {
            if (ex instanceof BadRequestException badRequest) {
                throw badRequest;
            }
            log.warn("Unreadable M-Pesa callback: {}", ex.getMessage());
            throw new BadRequestException("Unreadable callback");
        }
    }

    private static Optional<Instant> parseTime(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDateTime.parse(value.trim(), TRANS_TIME).atZone(TimeConfig.NAIROBI).toInstant());
        }
        catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }
}
