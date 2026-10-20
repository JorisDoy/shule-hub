package ke.shulehub.assessment.internal;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import ke.shulehub.shared.persistence.TenantScopedEntity;

@Entity
@Table(name = "assessment_score")
public class AssessmentScore extends TenantScopedEntity {

    @Column(name = "assessment_id", nullable = false, updatable = false)
    private UUID assessmentId;

    @Column(name = "learner_id", nullable = false, updatable = false)
    private UUID learnerId;

    @Column(name = "raw_score")
    private BigDecimal rawScore;

    @Column(name = "rubric_level")
    private String rubricLevel;

    @Column(name = "comment")
    private String comment;

    @Column(name = "recorded_by_person_id")
    private UUID recordedByPersonId;

    protected AssessmentScore() {
    }

    public AssessmentScore(UUID assessmentId, UUID learnerId) {
        this.assessmentId = assessmentId;
        this.learnerId = learnerId;
    }

    public void record(BigDecimal newRawScore, String newRubricLevel, String newComment, UUID recordedBy) {
        this.rawScore = newRawScore;
        this.rubricLevel = newRubricLevel;
        this.comment = newComment == null || newComment.isBlank() ? null : newComment.trim();
        this.recordedByPersonId = recordedBy;
    }

    public boolean isEmpty() {
        return rawScore == null && rubricLevel == null;
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public UUID getLearnerId() {
        return learnerId;
    }

    public BigDecimal getRawScore() {
        return rawScore;
    }

    public String getRubricLevel() {
        return rubricLevel;
    }

    public String getComment() {
        return comment;
    }
}
