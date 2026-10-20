package ke.shulehub.fees.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import ke.shulehub.fees.FeeAppliesTo;
import ke.shulehub.fees.FeeStructureStatus;
import ke.shulehub.learners.BoardingStatus;
import ke.shulehub.shared.money.Money;
import ke.shulehub.shared.persistence.TenantScopedEntity;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.web.ConflictException;

/** What one grade at one branch pays for one term. */
@Entity
@Table(name = "fee_structure")
public class FeeStructure extends TenantScopedEntity {

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    @Column(name = "term_id", nullable = false, updatable = false)
    private UUID termId;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_level", nullable = false, updatable = false)
    private GradeLevel gradeLevel;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FeeStructureStatus status = FeeStructureStatus.DRAFT;

    @OneToMany(mappedBy = "feeStructure", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeeStructureItem> items = new ArrayList<>();

    protected FeeStructure() {
    }

    public FeeStructure(UUID branchId, UUID termId, GradeLevel gradeLevel, String name) {
        this.branchId = branchId;
        this.termId = termId;
        this.gradeLevel = gradeLevel;
        this.name = name.trim();
    }

    public void replaceItems(String newName, List<FeeStructureItem.Draft> drafts) {
        requireDraft();
        this.name = newName.trim();
        items.clear();
        drafts.forEach(draft -> items.add(new FeeStructureItem(this, draft.voteHeadId(), Money.of(draft.amount()),
                draft.appliesTo())));
    }

    public void publish() {
        requireDraft();
        if (items.isEmpty()) {
            throw new ConflictException("Add at least one fee item before publishing");
        }
        status = FeeStructureStatus.PUBLISHED;
    }

    /** Items a learner with this boarding status is billed for. */
    public List<FeeStructureItem> itemsFor(BoardingStatus boardingStatus) {
        return items.stream().filter(item -> item.appliesTo(boardingStatus)).toList();
    }

    public BigDecimal totalFor(BoardingStatus boardingStatus) {
        return Money.sum(itemsFor(boardingStatus), FeeStructureItem::getAmount);
    }

    private void requireDraft() {
        if (status != FeeStructureStatus.DRAFT) {
            throw new ConflictException("Published fee structures can't be changed");
        }
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getTermId() {
        return termId;
    }

    public GradeLevel getGradeLevel() {
        return gradeLevel;
    }

    public String getName() {
        return name;
    }

    public FeeStructureStatus getStatus() {
        return status;
    }

    public List<FeeStructureItem> getItems() {
        return items;
    }

    /** Helper for callers that only need the applies-to rule. */
    public static boolean applies(FeeAppliesTo rule, BoardingStatus status) {
        return rule == FeeAppliesTo.ALL
                || (rule == FeeAppliesTo.DAY && status == BoardingStatus.DAY)
                || (rule == FeeAppliesTo.BOARDING && status == BoardingStatus.BOARDING);
    }
}
