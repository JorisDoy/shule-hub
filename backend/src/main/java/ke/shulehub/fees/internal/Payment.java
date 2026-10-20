package ke.shulehub.fees.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ke.shulehub.fees.PaymentChannel;
import ke.shulehub.fees.PaymentStatus;
import ke.shulehub.shared.money.Money;
import ke.shulehub.shared.persistence.TenantScopedEntity;
import ke.shulehub.shared.web.ConflictException;

/** Money received. The part not yet applied to invoices is the family's credit. */
@Entity
@Table(name = "payment")
public class Payment extends TenantScopedEntity {

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    @Column(name = "learner_id")
    private UUID learnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, updatable = false)
    private PaymentChannel channel;

    @Column(name = "reference", nullable = false, updatable = false)
    private String reference;

    /** M-Pesa receipts de-duplicate across STK and paybill; manual entries per channel. */
    @Column(name = "dedupe_key", nullable = false, updatable = false)
    private String dedupeKey;

    @Column(name = "amount", nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "amount_allocated", nullable = false)
    private BigDecimal amountAllocated = Money.ZERO;

    @Column(name = "payer_phone")
    private String payerPhone;

    @Column(name = "payer_name")
    private String payerName;

    @Column(name = "account_reference")
    private String accountReference;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "raw_payload", updatable = false)
    private String rawPayload;

    protected Payment() {
    }

    public Payment(UUID branchId, UUID learnerId, PaymentChannel channel, String reference, BigDecimal amount,
            String payerPhone, String payerName, String accountReference, Instant receivedAt, String rawPayload) {
        this.branchId = branchId;
        this.learnerId = learnerId;
        this.channel = channel;
        this.reference = reference.trim();
        this.dedupeKey = dedupeKey(channel, reference);
        this.amount = Money.of(amount);
        this.payerPhone = payerPhone;
        this.payerName = payerName;
        this.accountReference = accountReference;
        this.receivedAt = receivedAt;
        this.rawPayload = rawPayload;
        this.status = learnerId == null ? PaymentStatus.SUSPENSE : PaymentStatus.ALLOCATED;
    }

    public static String dedupeKey(PaymentChannel channel, String reference) {
        String normalized = reference.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (channel) {
            case MPESA_STK, MPESA_C2B -> "MPESA:" + normalized;
            case BANK, CASH -> channel.name() + ":" + normalized;
        };
    }

    public void assignTo(UUID newLearnerId) {
        if (status != PaymentStatus.SUSPENSE) {
            throw new ConflictException("Only unmatched (suspense) payments can be assigned");
        }
        this.learnerId = newLearnerId;
        this.status = PaymentStatus.ALLOCATED;
    }

    public void recordAllocation(BigDecimal allocated) {
        this.amountAllocated = amountAllocated.add(allocated);
    }

    public BigDecimal unallocated() {
        return amount.subtract(amountAllocated);
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getLearnerId() {
        return learnerId;
    }

    public PaymentChannel getChannel() {
        return channel;
    }

    public String getReference() {
        return reference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getAmountAllocated() {
        return amountAllocated;
    }

    public String getPayerPhone() {
        return payerPhone;
    }

    public String getPayerName() {
        return payerName;
    }

    public String getAccountReference() {
        return accountReference;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public PaymentStatus getStatus() {
        return status;
    }
}
