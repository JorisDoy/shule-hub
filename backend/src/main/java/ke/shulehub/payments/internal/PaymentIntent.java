package ke.shulehub.payments.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ke.shulehub.payments.PaymentIntentStatus;
import ke.shulehub.shared.persistence.TenantScopedEntity;

/** One STK Push prompt sent to a parent's phone, and how it ended. */
@Entity
@Table(name = "payment_intent")
public class PaymentIntent extends TenantScopedEntity {

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    @Column(name = "channel_id", nullable = false, updatable = false)
    private UUID channelId;

    @Column(name = "learner_id", nullable = false, updatable = false)
    private UUID learnerId;

    @Column(name = "invoice_id", updatable = false)
    private UUID invoiceId;

    @Column(name = "amount", nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "phone", nullable = false, updatable = false)
    private String phone;

    @Column(name = "account_reference", nullable = false, updatable = false)
    private String accountReference;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "merchant_request_id")
    private String merchantRequestId;

    @Column(name = "checkout_request_id")
    private String checkoutRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentIntentStatus status = PaymentIntentStatus.PENDING;

    @Column(name = "result_code")
    private String resultCode;

    @Column(name = "result_desc")
    private String resultDesc;

    @Column(name = "mpesa_receipt")
    private String mpesaReceipt;

    @Column(name = "requested_by_person_id", updatable = false)
    private UUID requestedByPersonId;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected PaymentIntent() {
    }

    public PaymentIntent(UUID branchId, UUID channelId, UUID learnerId, UUID invoiceId, BigDecimal amount, String phone,
            String accountReference, String idempotencyKey, UUID requestedByPersonId) {
        this.branchId = branchId;
        this.channelId = channelId;
        this.learnerId = learnerId;
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.phone = phone;
        this.accountReference = accountReference;
        this.idempotencyKey = idempotencyKey;
        this.requestedByPersonId = requestedByPersonId;
    }

    public void submitted(String merchantRequest, String checkoutRequest, String description) {
        this.merchantRequestId = merchantRequest;
        this.checkoutRequestId = checkoutRequest;
        this.status = PaymentIntentStatus.SUBMITTED;
        this.resultDesc = description;
    }

    public void succeeded(String receipt, String description, Instant at) {
        this.status = PaymentIntentStatus.SUCCEEDED;
        this.resultCode = "0";
        this.mpesaReceipt = receipt;
        this.resultDesc = description;
        this.completedAt = at;
    }

    public void failed(String code, String description, Instant at) {
        this.status = PaymentIntentStatus.FAILED;
        this.resultCode = code;
        this.resultDesc = description;
        this.completedAt = at;
    }

    public boolean isFinished() {
        return status == PaymentIntentStatus.SUCCEEDED || status == PaymentIntentStatus.FAILED;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public UUID getLearnerId() {
        return learnerId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPhone() {
        return phone;
    }

    public String getAccountReference() {
        return accountReference;
    }

    public String getCheckoutRequestId() {
        return checkoutRequestId;
    }

    public PaymentIntentStatus getStatus() {
        return status;
    }

    public String getResultDesc() {
        return resultDesc;
    }

    public String getMpesaReceipt() {
        return mpesaReceipt;
    }
}
