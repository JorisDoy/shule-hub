package ke.shulehub.payments;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A confirmed M-Pesa payment into a school's paybill or till. Published in the same
 * transaction that records the callback; the event publication registry re-delivers it until
 * the fees ledger has recorded it, so a crash between the two never loses money.
 *
 * @param receipt the M-Pesa receipt number, unique per transaction (used to de-duplicate)
 * @param learnerId the learner the money is for, or null when the account number matched nobody
 * @param invoiceId the invoice the parent chose to pay (STK only), or null
 * @param intentId the STK payment intent, or null for paybill payments
 */
public record MpesaPaymentReceived(UUID tenantId, UUID branchId, Source source, String receipt, BigDecimal amount,
        String payerPhone, String payerName, String accountReference, UUID learnerId, UUID invoiceId, UUID intentId,
        Instant receivedAt, String rawPayload) {

    public enum Source {
        STK,
        C2B
    }
}
