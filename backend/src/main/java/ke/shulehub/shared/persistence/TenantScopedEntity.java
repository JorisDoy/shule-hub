package ke.shulehub.shared.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.TenantId;

/**
 * Base for rows that belong to one tenant. Hibernate sets {@code tenantId} on insert from
 * the session's tenant and filters every query and load by it.
 */
@MappedSuperclass
public abstract class TenantScopedEntity extends BaseEntity {

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    public UUID getTenantId() {
        return tenantId;
    }
}
