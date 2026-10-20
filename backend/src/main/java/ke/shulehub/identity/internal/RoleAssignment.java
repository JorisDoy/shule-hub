package ke.shulehub.identity.internal;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ke.shulehub.identity.Grant;
import ke.shulehub.identity.Role;
import ke.shulehub.identity.ScopeType;
import ke.shulehub.shared.persistence.TenantScopedEntity;

@Entity
@Table(name = "role_assignment")
public class RoleAssignment extends TenantScopedEntity {

    @Column(name = "person_id", nullable = false, updatable = false)
    private UUID personId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private ScopeType scopeType;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "stream_id")
    private UUID streamId;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    protected RoleAssignment() {
    }

    public RoleAssignment(UUID personId, Grant grant) {
        this.personId = personId;
        this.role = grant.role();
        this.scopeType = grant.scopeType();
        this.branchId = grant.branchId();
        this.streamId = grant.streamId();
    }

    public Grant toGrant() {
        return new Grant(role, scopeType, branchId, streamId);
    }

    public boolean activeOn(LocalDate date) {
        return (validFrom == null || !date.isBefore(validFrom)) && (validTo == null || !date.isAfter(validTo));
    }

    public UUID getPersonId() {
        return personId;
    }

    public UUID getBranchId() {
        return branchId;
    }
}
