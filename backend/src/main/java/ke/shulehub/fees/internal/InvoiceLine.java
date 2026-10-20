package ke.shulehub.fees.internal;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import ke.shulehub.shared.money.Money;
import ke.shulehub.shared.persistence.TenantScopedEntity;

@Entity
@Table(name = "invoice_line")
public class InvoiceLine extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private Invoice invoice;

    @Column(name = "vote_head_id", nullable = false, updatable = false)
    private UUID voteHeadId;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "amount_paid", nullable = false)
    private BigDecimal amountPaid = Money.ZERO;

    protected InvoiceLine() {
    }

    InvoiceLine(Invoice invoice, UUID voteHeadId, String description, int priority, BigDecimal amount) {
        this.invoice = invoice;
        this.voteHeadId = voteHeadId;
        this.description = description;
        this.priority = priority;
        this.amount = amount;
    }

    void apply(BigDecimal payment) {
        if (payment.signum() <= 0 || payment.compareTo(outstanding()) > 0) {
            throw new IllegalArgumentException("Cannot apply " + payment + " to a line with " + outstanding() + " outstanding");
        }
        amountPaid = amountPaid.add(payment);
    }

    public BigDecimal outstanding() {
        return amount.subtract(amountPaid);
    }

    public UUID getVoteHeadId() {
        return voteHeadId;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }
}
