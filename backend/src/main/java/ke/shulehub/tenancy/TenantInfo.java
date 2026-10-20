package ke.shulehub.tenancy;

import java.util.UUID;

public record TenantInfo(UUID id, String name, String slug) {
}
