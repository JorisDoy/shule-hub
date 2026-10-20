package ke.shulehub.assessment.internal;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import ke.shulehub.shared.persistence.TenantScopedEntity;

@Entity
@Table(name = "report_card_area_comment")
public class ReportCardAreaComment extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_card_id", nullable = false, updatable = false)
    private ReportCard reportCard;

    @Column(name = "learning_area_id", nullable = false, updatable = false)
    private UUID learningAreaId;

    @Column(name = "comment", nullable = false)
    private String comment;

    protected ReportCardAreaComment() {
    }

    ReportCardAreaComment(ReportCard reportCard, UUID learningAreaId, String comment) {
        this.reportCard = reportCard;
        this.learningAreaId = learningAreaId;
        this.comment = comment;
    }

    void setComment(String comment) {
        this.comment = comment;
    }

    public UUID getLearningAreaId() {
        return learningAreaId;
    }

    public String getComment() {
        return comment;
    }
}
