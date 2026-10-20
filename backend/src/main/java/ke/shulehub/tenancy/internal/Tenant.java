package ke.shulehub.tenancy.internal;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import ke.shulehub.shared.persistence.BaseEntity;

/** A customer: one school or a group of branches under one owner. */
@Entity
@Table(name = "tenant")
public class Tenant extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    protected Tenant() {
    }

    public Tenant(String name, String slug) {
        this.name = name.trim();
        this.slug = slug;
    }

    /** Tenants are created with a known id so the provisioning transaction can run as that tenant. */
    public static Tenant withId(UUID id, String name, String slug) {
        Tenant tenant = new Tenant(name, slug);
        tenant.assignId(id);
        return tenant;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getStatus() {
        return status;
    }
}
