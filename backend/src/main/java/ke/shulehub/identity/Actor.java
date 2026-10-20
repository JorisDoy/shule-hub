package ke.shulehub.identity;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The signed-in caller for this request.
 *
 * @param person the linked person; null only for a platform admin with no school account
 * @param grants roles held in the tenant named by {@code X-Tenant-Id} (empty without one)
 */
public record Actor(PersonRef person, List<Grant> grants, boolean platformAdmin) {

    public Actor {
        grants = List.copyOf(grants);
    }

    public UUID personId() {
        return person == null ? null : person.id();
    }

    public Set<Permission> permissions() {
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        grants.forEach(grant -> permissions.addAll(grant.role().permissions()));
        return permissions;
    }

    public boolean hasRole(Role role) {
        return grants.stream().anyMatch(grant -> grant.role() == role);
    }
}
