package ke.shulehub.tenancy;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ke.shulehub.identity.Grant;
import ke.shulehub.identity.PersonDirectory;
import ke.shulehub.identity.PersonRef;
import ke.shulehub.identity.Role;
import ke.shulehub.identity.RoleAssignments;
import ke.shulehub.shared.tenancy.TenantContext;
import ke.shulehub.shared.web.ConflictException;
import ke.shulehub.tenancy.internal.Tenant;
import ke.shulehub.tenancy.internal.TenantRepository;

/**
 * Creates a tenant with its first branch and director. Listeners in other modules seed
 * defaults in the same transaction, so a half-provisioned school never exists.
 */
@Service
public class TenantProvisioning {

    public record NewBranch(String name, String code, String county, boolean boardingEnabled) {
    }

    public record NewDirector(String givenName, String familyName, String email, String phone) {
    }

    public record Provisioned(UUID tenantId, UUID branchId, UUID directorPersonId) {
    }

    private final TenantRepository tenants;
    private final BranchDirectory branches;
    private final PersonDirectory people;
    private final RoleAssignments roles;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactions;

    public TenantProvisioning(TenantRepository tenants, BranchDirectory branches, PersonDirectory people,
            RoleAssignments roles, ApplicationEventPublisher events, PlatformTransactionManager transactionManager) {
        this.tenants = tenants;
        this.branches = branches;
        this.people = people;
        this.roles = roles;
        this.events = events;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /**
     * Runs the provisioning transaction as the new tenant. Callers authorise first (platform
     * admin, or the demo seeder at start-up).
     */
    public Provisioned provision(String name, String slug, NewBranch branch, NewDirector director) {
        UUID tenantId = UUID.randomUUID();
        // The tenant must be bound before the transaction opens its Hibernate session.
        return TenantContext.callAs(tenantId,
                () -> transactions.execute(status -> provisionAs(tenantId, name, slug, branch, director)));
    }

    private Provisioned provisionAs(UUID tenantId, String name, String slug, NewBranch branch, NewDirector director) {
        String normalizedSlug = slug.trim().toLowerCase(java.util.Locale.ROOT);
        if (tenants.existsBySlug(normalizedSlug)) {
            throw new ConflictException("The address " + normalizedSlug + " is already taken");
        }
        tenants.saveAndFlush(Tenant.withId(tenantId, name, normalizedSlug));
        BranchInfo firstBranch = branches.createUnchecked(branch.name(), branch.code(), branch.county(),
                branch.boardingEnabled());
        PersonRef directorPerson = people.findOrCreate(director.givenName(), director.familyName(), director.phone(),
                director.email());
        roles.assign(directorPerson.id(), Grant.group(Role.GROUP_DIRECTOR));
        events.publishEvent(new TenantProvisioned(tenantId, firstBranch.id(), directorPerson.id()));
        return new Provisioned(tenantId, firstBranch.id(), directorPerson.id());
    }
}
