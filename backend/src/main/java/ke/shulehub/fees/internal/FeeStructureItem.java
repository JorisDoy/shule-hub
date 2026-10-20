package ke.shulehub.fees.internal;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import ke.shulehub.fees.FeeAppliesTo;
import ke.shulehub.learners.BoardingStatus;
import ke.shulehub.shared.persistence.TenantScopedEntity;

@Entity
@Table(name = "fee_structure_item")
public class FeeStructureItem extends TenantScopedEntity {

    public record Draft(UUID voteHeadId, BigDecimal amount, FeeAppliesTo appliesTo) {
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fee_structure_id", nullable = false, updatable = false)
    private FeeStructure feeStructure;

    @Column(name = "vote_head_id", nullable = false)
    private UUID voteHeadId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to", nullable = false)
    private FeeAppliesTo appliesTo;

    protected FeeStructureItem() {
    }

    FeeStructureItem(FeeStructure feeStructure, UUID voteHeadId, BigDecimal amount, FeeAppliesTo appliesTo) {
        this.feeStructure = feeStructure;
        this.voteHeadId = voteHeadId;
        this.amount = amount;
        this.appliesTo = appliesTo == null ? FeeAppliesTo.ALL : appliesTo;
    }

    public boolean appliesTo(BoardingStatus boardingStatus) {
        return FeeStructure.applies(appliesTo, boardingStatus);
    }

    public UUID getVoteHeadId() {
        return voteHeadId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public FeeAppliesTo getAppliesTo() {
        return appliesTo;
    }
}
