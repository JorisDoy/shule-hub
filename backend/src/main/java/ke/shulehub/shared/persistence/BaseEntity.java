package ke.shulehub.shared.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

/**
 * Base for every entity: an application-assigned UUID (known before insert, so it can go
 * into events), optimistic locking and audit timestamps.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void beforeInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    /** Overrides the generated id before the first save, when the id must be known up front. */
    protected void assignId(UUID assigned) {
        if (version != null) {
            throw new IllegalStateException("Cannot change the id of a saved entity");
        }
        this.id = assigned;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Objects.equals(id, ((BaseEntity) other).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
