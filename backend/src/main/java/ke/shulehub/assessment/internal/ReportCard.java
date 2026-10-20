package ke.shulehub.assessment.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import ke.shulehub.assessment.ReportCardStatus;
import ke.shulehub.shared.persistence.TenantScopedEntity;
import ke.shulehub.shared.web.ConflictException;

/**
 * The comments and release state of one learner's term report. Results themselves are
 * computed from assessments each time, so a late correction shows up until release.
 */
@Entity
@Table(name = "report_card")
public class ReportCard extends TenantScopedEntity {

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    @Column(name = "learner_id", nullable = false, updatable = false)
    private UUID learnerId;

    @Column(name = "term_id", nullable = false, updatable = false)
    private UUID termId;

    @Column(name = "stream_id", nullable = false)
    private UUID streamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportCardStatus status = ReportCardStatus.DRAFT;

    @Column(name = "class_teacher_comment")
    private String classTeacherComment;

    @Column(name = "head_comment")
    private String headComment;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by_person_id")
    private UUID approvedByPersonId;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "released_by_person_id")
    private UUID releasedByPersonId;

    @OneToMany(mappedBy = "reportCard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportCardAreaComment> areaComments = new ArrayList<>();

    protected ReportCard() {
    }

    public ReportCard(UUID branchId, UUID learnerId, UUID termId, UUID streamId) {
        this.branchId = branchId;
        this.learnerId = learnerId;
        this.termId = termId;
        this.streamId = streamId;
    }

    public void requireEditable() {
        if (status == ReportCardStatus.RELEASED) {
            throw new ConflictException("This report card has been released to parents and can't be changed");
        }
    }

    public void setClassTeacherComment(String comment) {
        requireEditable();
        this.classTeacherComment = blankToNull(comment);
    }

    public void setHeadComment(String comment) {
        requireEditable();
        this.headComment = blankToNull(comment);
    }

    public void setAreaComment(UUID learningAreaId, String comment) {
        requireEditable();
        Optional<ReportCardAreaComment> existing = areaComments.stream()
                .filter(c -> c.getLearningAreaId().equals(learningAreaId)).findFirst();
        String text = blankToNull(comment);
        if (text == null) {
            existing.ifPresent(areaComments::remove);
        }
        else if (existing.isPresent()) {
            existing.get().setComment(text);
        }
        else {
            areaComments.add(new ReportCardAreaComment(this, learningAreaId, text));
        }
    }

    public Optional<String> areaComment(UUID learningAreaId) {
        return areaComments.stream().filter(c -> c.getLearningAreaId().equals(learningAreaId))
                .map(ReportCardAreaComment::getComment).findFirst();
    }

    public void approve(UUID personId, Instant at) {
        requireEditable();
        status = ReportCardStatus.APPROVED;
        approvedByPersonId = personId;
        approvedAt = at;
    }

    public void release(UUID personId, Instant at) {
        if (status != ReportCardStatus.APPROVED) {
            throw new ConflictException("Approve the report card before releasing it");
        }
        status = ReportCardStatus.RELEASED;
        releasedByPersonId = personId;
        releasedAt = at;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

    public UUID getStreamId() {
        return streamId;
    }

    public ReportCardStatus getStatus() {
        return status;
    }

    public String getClassTeacherComment() {
        return classTeacherComment;
    }

    public String getHeadComment() {
        return headComment;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }
}
