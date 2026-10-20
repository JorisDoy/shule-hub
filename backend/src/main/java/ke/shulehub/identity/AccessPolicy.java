package ke.shulehub.identity;

import java.util.UUID;

import org.springframework.stereotype.Component;

import ke.shulehub.shared.web.ForbiddenException;

/** Authorisation checks every module calls before acting. */
@Component
public class AccessPolicy {

    /** Requires {@code permission} across the whole tenant (only group-level grants qualify). */
    public void requireTenantWide(Permission permission) {
        require(permission, null, null);
    }

    public void require(Permission permission, UUID branchId) {
        require(permission, branchId, null);
    }

    public void require(Permission permission, UUID branchId, UUID streamId) {
        if (!has(permission, branchId, streamId)) {
            throw new ForbiddenException("You don't have permission to do this (" + permission + ")");
        }
    }

    public boolean has(Permission permission, UUID branchId) {
        return has(permission, branchId, null);
    }

    public boolean has(Permission permission, UUID branchId, UUID streamId) {
        return ActorContext.current()
                .map(actor -> actor.grants().stream().anyMatch(grant -> grant.allows(permission, branchId, streamId)))
                .orElse(false);
    }

    public void requirePlatformAdmin() {
        if (!ActorContext.require().platformAdmin()) {
            throw new ForbiddenException("Only Shule Hub platform administrators can do this");
        }
    }
}
