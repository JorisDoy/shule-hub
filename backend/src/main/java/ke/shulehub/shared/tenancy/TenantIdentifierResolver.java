package ke.shulehub.shared.tenancy;

import java.util.UUID;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Tells Hibernate which tenant a new session belongs to. Registered by class name in
 * {@code spring.jpa.properties.hibernate.tenant_identifier_resolver}; Hibernate then adds
 * {@code tenant_id = ?} to every query and load of an entity with an {@code @TenantId}
 * field, and fills that field on insert.
 */
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.resolveForHibernate();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public boolean isRoot(UUID tenantId) {
        return TenantContext.ROOT.equals(tenantId);
    }
}
