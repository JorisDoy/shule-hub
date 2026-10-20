package ke.shulehub.fees.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import ke.shulehub.fees.InvoiceStatus;
import ke.shulehub.shared.money.Money;
import ke.shulehub.shared.persistence.TenantScopedEntity;

/** A bill for one learner and term. Lines carry the vote heads; payments are applied per line. */
@Entity
@Table(name = "invoice")
public class Invoice extends TenantScopedEntity {

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    @Column(name = "learner_id", nullable = false, updatable = false)
    private UUID learnerId;

    @Column(name = "term_id", nullable = false, updatable = false)
    private UUID termId;

    @Column(name = "fee_structure_id", updatable = false)
    private UUID feeStructureId;

    @Column(name = "number", nullable = false, updatable = false)
    private String number;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    @Column(name = "due_on", nullable = false)
    private LocalDate dueOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status = InvoiceStatus.OPEN;

    @Column(name = "total", nullable = false)
    private BigDecimal total = Money.ZERO;

    @Column(name = "amount_paid", nullable = false)
    private BigDecimal amountPaid = Money.ZERO;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("priority ASC, description ASC")
    private List<InvoiceLine> lines = new ArrayList<>();

    protected Invoice() {
    }

    public Invoice(UUID branchId, UUID learnerId, UUID termId, UUID feeStructureId, String number, String description,
            LocalDate issuedOn, LocalDate dueOn) {
        this.branchId = branchId;
        this.learnerId = learnerId;
        this.termId = termId;
        this.feeStructureId = feeStructureId;
        this.number = number;
        this.description = description;
        this.issuedOn = issuedOn;
        this.dueOn = dueOn;
    }

    public void addLine(UUID voteHeadId, String lineDescription, int priority, BigDecimal amount) {
        lines.add(new InvoiceLine(this, voteHeadId, lineDescription, priority, Money.of(amount)));
        total = Money.sum(lines, InvoiceLine::getAmount);
    }

    /** Applies part of a payment to one line and updates the invoice's paid amount and status. */
    public void applyToLine(UUID lineId, BigDecimal amount) {
        InvoiceLine line = lines.stream().filter(candidate -> candidate.getId().equals(lineId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Line " + lineId + " is not on invoice " + number));
        line.apply(amount);
        amountPaid = Money.sum(lines, InvoiceLine::getAmountPaid);
        status = amountPaid.compareTo(total) >= 0 ? InvoiceStatus.PAID
                : amountPaid.signum() > 0 ? InvoiceStatus.PARTIALLY_PAID : InvoiceStatus.OPEN;
    }

    public BigDecimal balance() {
        return total.subtract(amountPaid);
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getLearnerId() {
        return learnerId;
    }

    public UUID getTermId() {
        return termId;
    }

    public String getNumber() {
        return number;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public List<InvoiceLine> getLines() {
        return lines;
    }
}
