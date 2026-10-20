package ke.shulehub.assessment.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ke.shulehub.assessment.AssessmentStatus;
import ke.shulehub.assessment.AssessmentType;
import ke.shulehub.shared.persistence.TenantScopedEntity;
import ke.shulehub.shared.web.ConflictException;

/**
 * A CAT, end-term exam, observation or project for one stream and learning area. With a
 * maximum score, teachers enter marks; without one, they pick a band (rubric) per learner.
 */
@Entity
@Table(name = "assessment")
public class Assessment extends TenantScopedEntity {

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    @Column(name = "term_id", nullable = false, updatable = false)
    private UUID termId;

    @Column(name = "stream_id", nullable = false, updatable = false)
    private UUID streamId;

    @Column(name = "learning_area_id", nullable = false, updatable = false)
    private UUID learningAreaId;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_type", nullable = false)
    private AssessmentType type;

    @Column(name = "max_score")
    private BigDecimal maxScore;

    @Column(name = "weight", nullable = false)
    private BigDecimal weight;

    @Column(name = "assessed_on", nullable = false)
    private LocalDate assessedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AssessmentStatus status = AssessmentStatus.DRAFT;

    @Column(name = "created_by_person_id", updatable = false)
    private UUID createdByPersonId;

    protected Assessment() {
    }

    public Assessment(UUID branchId, UUID termId, UUID streamId, UUID learningAreaId, String title, AssessmentType type,
            BigDecimal maxScore, BigDecimal weight, LocalDate assessedOn, UUID createdByPersonId) {
        this.branchId = branchId;
        this.termId = termId;
        this.streamId = streamId;
        this.learningAreaId = learningAreaId;
        this.title = title.trim();
        this.type = type;
        this.maxScore = maxScore;
        this.weight = weight;
        this.assessedOn = assessedOn;
        this.createdByPersonId = createdByPersonId;
    }

    public boolean isRubric() {
        return maxScore == null;
    }

    public void requireEditable() {
        if (status == AssessmentStatus.RELEASED) {
            throw new ConflictException("Scores for a released assessment can't be changed");
        }
    }

    public void release() {
        requireEditable();
        status = AssessmentStatus.RELEASED;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getTermId() {
        return termId;
    }

    public UUID getStreamId() {
        return streamId;
    }

    public UUID getLearningAreaId() {
        return learningAreaId;
    }

    public String getTitle() {
        return title;
    }

    public AssessmentType getType() {
        return type;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public LocalDate getAssessedOn() {
        return assessedOn;
    }

    public AssessmentStatus getStatus() {
        return status;
    }
}
