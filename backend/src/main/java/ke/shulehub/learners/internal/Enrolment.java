package ke.shulehub.learners.internal;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import ke.shulehub.shared.persistence.TenantScopedEntity;

/** A learner's place in a stream for one academic year. */
@Entity
@Table(name = "enrolment")
public class Enrolment extends TenantScopedEntity {

    public static final String ACTIVE = "ACTIVE";

    @Column(name = "learner_id", nullable = false, updatable = false)
    private UUID learnerId;

    @Column(name = "stream_id", nullable = false)
    private UUID streamId;

    @Column(name = "academic_year_id", nullable = false, updatable = false)
    private UUID academicYearId;

    @Column(name = "status", nullable = false)
    private String status = ACTIVE;

    @Column(name = "enrolled_on", nullable = false)
    private LocalDate enrolledOn;

    protected Enrolment() {
    }

    public Enrolment(UUID learnerId, UUID streamId, UUID academicYearId, LocalDate enrolledOn) {
        this.learnerId = learnerId;
        this.streamId = streamId;
        this.academicYearId = academicYearId;
        this.enrolledOn = enrolledOn;
    }

    public UUID getLearnerId() {
        return learnerId;
    }

    public UUID getStreamId() {
        return streamId;
    }

    public UUID getAcademicYearId() {
        return academicYearId;
    }

    public LocalDate getEnrolledOn() {
        return enrolledOn;
    }
}
