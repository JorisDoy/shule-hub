package ke.shulehub.tenancy;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.tenancy.internal.Tenant;
import ke.shulehub.tenancy.internal.TenantRepository;

/** Tenant lookups. The tenant table is not tenant-scoped, so these work in any context. */
@Service
public class TenantDirectory {

    private final TenantRepository tenants;

    public TenantDirectory(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @Transactional(readOnly = true)
    public Optional<TenantInfo> findBySlug(String slug) {
        return tenants.findBySlug(slug).filter(tenant -> "ACTIVE".equals(tenant.getStatus())).map(TenantDirectory::toInfo);
    }

    @Transactional(readOnly = true)
    public List<TenantInfo> findAll(Collection<UUID> ids) {
        return tenants.findAllById(ids).stream().map(TenantDirectory::toInfo).toList();
    }

    private static TenantInfo toInfo(Tenant tenant) {
        return new TenantInfo(tenant.getId(), tenant.getName(), tenant.getSlug());
    }
}
