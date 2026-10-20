package ke.shulehub.tenancy.internal;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import ke.shulehub.shared.persistence.TenantScopedEntity;
import ke.shulehub.tenancy.BranchInfo;

/** One campus of a school group. Its code prefixes admission numbers (e.g. RUA0042). */
@Entity
@Table(name = "branch")
public class Branch extends TenantScopedEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "county")
    private String county;

    @Column(name = "boarding_enabled", nullable = false)
    private boolean boardingEnabled;

    protected Branch() {
    }

    public Branch(String name, String code, String county, boolean boardingEnabled) {
        this.name = name.trim();
        this.code = normalizeCode(code);
        this.county = county;
        this.boardingEnabled = boardingEnabled;
    }

    public static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    public BranchInfo toInfo() {
        return new BranchInfo(getId(), name, code, county, boardingEnabled);
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }
}
