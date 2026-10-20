package ke.shulehub.shared.tenancy;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import ke.shulehub.shared.web.BadRequestException;

/**
 * The tenant (school group) the current thread acts for.
 *
 * <p>Hibernate reads this when it opens a session, so it must be set <em>before</em> a
 * transaction starts. Web requests get it from the {@code X-Tenant-Id} header; async
 * listeners restore it from the event they handle.
 */
public final class TenantContext {

    /** Matches no tenant: queries return nothing and inserts fail on the tenant foreign key. */
    public static final UUID NONE = new UUID(0L, 0L);

    /** Sees every tenant. Only for narrow system lookups such as resolving an M-Pesa callback. */
    public static final UUID ROOT = new UUID(-1L, -1L);

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    /** The current tenant, or empty when none (or root) is set. */
    public static Optional<UUID> current() {
        UUID id = CURRENT.get();
        if (id == null || ROOT.equals(id) || NONE.equals(id)) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    /** The current tenant, or a 400 telling the client to send {@code X-Tenant-Id}. */
    public static UUID require() {
        return current().orElseThrow(() -> new BadRequestException("The X-Tenant-Id header is required for this request"));
    }

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static <T> T callAs(UUID tenantId, Supplier<T> action) {
        UUID previous = CURRENT.get();
        CURRENT.set(tenantId);
        try {
            return action.get();
        }
        finally {
            if (previous == null) {
                CURRENT.remove();
            }
            else {
                CURRENT.set(previous);
            }
        }
    }

    public static void runAs(UUID tenantId, Runnable action) {
        callAs(tenantId, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T callAsRoot(Supplier<T> action) {
        return callAs(ROOT, action);
    }

    static UUID resolveForHibernate() {
        UUID id = CURRENT.get();
        return id == null ? NONE : id;
    }
}
