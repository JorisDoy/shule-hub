package ke.shulehub.admissions.internal;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ke.shulehub.admissions.ApplicationStatus;
import ke.shulehub.shared.persistence.TenantScopedEntity;

/** One step in an application's history. */
@Entity
@Table(name = "admission_event")
public class AdmissionEvent extends TenantScopedEntity {

    @Column(name = "application_id", nullable = false, updatable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private ApplicationStatus toStatus;

    @Column(name = "note")
    private String note;

    @Column(name = "actor_person_id")
    private UUID actorPersonId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AdmissionEvent() {
    }

    public AdmissionEvent(UUID applicationId, ApplicationStatus fromStatus, ApplicationStatus toStatus, String note,
            UUID actorPersonId, Instant occurredAt) {
        this.applicationId = applicationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
        this.actorPersonId = actorPersonId;
        this.occurredAt = occurredAt;
    }

    public ApplicationStatus getFromStatus() {
        return fromStatus;
    }

    public ApplicationStatus getToStatus() {
        return toStatus;
    }

    public String getNote() {
        return note;
    }

    public UUID getActorPersonId() {
        return actorPersonId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
