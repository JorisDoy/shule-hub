package ke.shulehub.learners.internal;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ke.shulehub.learners.Relationship;
import ke.shulehub.shared.persistence.TenantScopedEntity;

@Entity
@Table(name = "guardian_link")
public class GuardianLink extends TenantScopedEntity {

    @Column(name = "learner_id", nullable = false, updatable = false)
    private UUID learnerId;

    @Column(name = "person_id", nullable = false, updatable = false)
    private UUID personId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", nullable = false)
    private Relationship relationship;

    @Column(name = "fee_payer", nullable = false)
    private boolean feePayer;

    @Column(name = "can_pick_up", nullable = false)
    private boolean canPickUp;

    @Column(name = "primary_contact", nullable = false)
    private boolean primaryContact;

    protected GuardianLink() {
    }

    public GuardianLink(UUID learnerId, UUID personId, Relationship relationship, boolean feePayer, boolean canPickUp,
            boolean primaryContact) {
        this.learnerId = learnerId;
        this.personId = personId;
        this.relationship = relationship;
        this.feePayer = feePayer;
        this.canPickUp = canPickUp;
        this.primaryContact = primaryContact;
    }

    public UUID getLearnerId() {
        return learnerId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public Relationship getRelationship() {
        return relationship;
    }

    public boolean isFeePayer() {
        return feePayer;
    }

    public boolean isCanPickUp() {
        return canPickUp;
    }

    public boolean isPrimaryContact() {
        return primaryContact;
    }
}
