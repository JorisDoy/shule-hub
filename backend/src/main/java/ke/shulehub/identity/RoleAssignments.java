package ke.shulehub.identity;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.identity.internal.RoleAssignment;
import ke.shulehub.identity.internal.RoleAssignmentRepository;
import ke.shulehub.shared.tenancy.TenantContext;

/** Grants roles and answers "what can this person do here?". */
@Service
public class RoleAssignments {

    private final RoleAssignmentRepository assignments;
    private final Clock clock;

    public RoleAssignments(RoleAssignmentRepository assignments, Clock clock) {
        this.assignments = assignments;
        this.clock = clock;
    }

    /** Grants a role in the current tenant; does nothing if the person already holds it. */
    @Transactional
    public void assign(UUID personId, Grant grant) {
        boolean held = assignments.findByPersonId(personId).stream()
                .anyMatch(existing -> existing.toGrant().equals(grant));
        if (!held) {
            assignments.save(new RoleAssignment(personId, grant));
        }
    }

    /** Active grants in the current tenant. */
    @Transactional(readOnly = true)
    public List<Grant> grantsFor(UUID personId) {
        LocalDate today = LocalDate.now(clock);
        return assignments.findByPersonId(personId).stream()
                .filter(assignment -> assignment.activeOn(today))
                .map(RoleAssignment::toGrant)
                .toList();
    }

    /**
     * Active grants in every tenant, keyed by tenant id. Runs in the root context, so it must
     * not be called inside a transaction that is already bound to a tenant.
     */
    public Map<UUID, List<Grant>> grantsAcrossTenants(UUID personId) {
        LocalDate today = LocalDate.now(clock);
        List<RoleAssignment> all = TenantContext.callAsRoot(() -> assignments.findByPersonId(personId));
        Map<UUID, List<Grant>> byTenant = new LinkedHashMap<>();
        all.stream()
                .filter(assignment -> assignment.activeOn(today))
                .forEach(assignment -> byTenant
                        .computeIfAbsent(assignment.getTenantId(), id -> new java.util.ArrayList<>())
                        .add(assignment.toGrant()));
        return byTenant;
    }
}
