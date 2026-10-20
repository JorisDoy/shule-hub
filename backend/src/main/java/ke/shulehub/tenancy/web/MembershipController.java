package ke.shulehub.tenancy.web;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ke.shulehub.identity.ActorContext;
import ke.shulehub.identity.Grant;
import ke.shulehub.identity.Permission;
import ke.shulehub.identity.PersonRef;
import ke.shulehub.identity.Role;
import ke.shulehub.identity.RoleAssignments;
import ke.shulehub.identity.ScopeType;
import ke.shulehub.tenancy.BranchDirectory;
import ke.shulehub.tenancy.BranchInfo;
import ke.shulehub.tenancy.TenantDirectory;
import ke.shulehub.tenancy.TenantInfo;

/** Which schools the signed-in person belongs to and what they can do in each. */
@RestController
class MembershipController {

    record GrantView(Role role, ScopeType scopeType, UUID branchId, String branchName, UUID streamId) {
    }

    record Membership(UUID tenantId, String tenantName, String tenantSlug, List<GrantView> grants,
            Set<Permission> permissions, boolean parent) {
    }

    private final RoleAssignments roleAssignments;
    private final TenantDirectory tenants;
    private final BranchDirectory branches;

    MembershipController(RoleAssignments roleAssignments, TenantDirectory tenants, BranchDirectory branches) {
        this.roleAssignments = roleAssignments;
        this.tenants = tenants;
        this.branches = branches;
    }

    @GetMapping("/api/me/memberships")
    List<Membership> memberships() {
        PersonRef person = ActorContext.requirePerson();
        Map<UUID, List<Grant>> grantsByTenant = roleAssignments.grantsAcrossTenants(person.id());
        if (grantsByTenant.isEmpty()) {
            return List.of();
        }
        Map<UUID, BranchInfo> branchNames = branches.findAcrossTenants(grantsByTenant.values().stream()
                .flatMap(List::stream)
                .map(Grant::branchId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        return tenants.findAll(grantsByTenant.keySet()).stream()
                .sorted(Comparator.comparing(TenantInfo::name))
                .map(tenant -> toMembership(tenant, grantsByTenant.get(tenant.id()), branchNames))
                .toList();
    }

    private static Membership toMembership(TenantInfo tenant, List<Grant> grants, Map<UUID, BranchInfo> branchNames) {
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        grants.forEach(grant -> permissions.addAll(grant.role().permissions()));
        List<GrantView> views = grants.stream()
                .map(grant -> new GrantView(grant.role(), grant.scopeType(), grant.branchId(),
                        grant.branchId() == null || !branchNames.containsKey(grant.branchId()) ? null
                                : branchNames.get(grant.branchId()).name(),
                        grant.streamId()))
                .toList();
        boolean parent = grants.stream().anyMatch(grant -> grant.role() == Role.PARENT);
        return new Membership(tenant.id(), tenant.name(), tenant.slug(), views, permissions, parent);
    }
}
