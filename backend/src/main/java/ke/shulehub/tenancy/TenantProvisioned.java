package ke.shulehub.tenancy;

import java.util.UUID;

/**
 * Published inside the provisioning transaction. Modules listen synchronously to seed their
 * defaults (learning areas, vote heads, grading scales) for the new tenant.
 */
public record TenantProvisioned(UUID tenantId, UUID branchId, UUID directorPersonId) {
}
