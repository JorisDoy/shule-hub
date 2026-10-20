package ke.shulehub.identity;

import java.util.Objects;
import java.util.UUID;

/** A role held at a scope within the current tenant. */
public record Grant(Role role, ScopeType scopeType, UUID branchId, UUID streamId) {

    public Grant {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(scopeType, "scopeType");
    }

    public static Grant group(Role role) {
        return new Grant(role, ScopeType.GROUP, null, null);
    }

    public static Grant branch(Role role, UUID branchId) {
        return new Grant(role, ScopeType.BRANCH, Objects.requireNonNull(branchId), null);
    }

    public static Grant stream(Role role, UUID branchId, UUID streamId) {
        return new Grant(role, ScopeType.STREAM, Objects.requireNonNull(branchId), Objects.requireNonNull(streamId));
    }

    /**
     * Whether this grant allows {@code permission} on a target. A null {@code branchId} means a
     * tenant-wide target, which only group grants cover; a stream grant covers only its stream.
     */
    public boolean allows(Permission permission, UUID targetBranchId, UUID targetStreamId) {
        if (!role.grants(permission)) {
            return false;
        }
        return switch (scopeType) {
            case GROUP -> true;
            case BRANCH -> targetBranchId != null && targetBranchId.equals(branchId);
            case STREAM -> targetBranchId != null && targetBranchId.equals(branchId)
                    && targetStreamId != null && targetStreamId.equals(streamId);
        };
    }
}
