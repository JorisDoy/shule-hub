package ke.shulehub.fees.internal;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import ke.shulehub.shared.persistence.TenantScopedEntity;

/** How much of a payment went to one invoice line. */
@Entity
@Table(name = "payment_allocation")
public class PaymentAllocation extends TenantScopedEntity {

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "invoice_line_id", nullable = false, updatable = false)
    private UUID invoiceLineId;

    @Column(name = "amount", nullable = false, updatable = false)
    private BigDecimal amount;

    protected PaymentAllocation() {
    }

    public PaymentAllocation(UUID paymentId, UUID invoiceLineId, BigDecimal amount) {
        this.paymentId = paymentId;
        this.invoiceLineId = invoiceLineId;
        this.amount = amount;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getInvoiceLineId() {
        return invoiceLineId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
