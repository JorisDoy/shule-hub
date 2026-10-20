package ke.shulehub.tenancy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.identity.AccessPolicy;
import ke.shulehub.identity.Permission;
import ke.shulehub.shared.tenancy.TenantContext;
import ke.shulehub.shared.web.BadRequestException;
import ke.shulehub.shared.web.ConflictException;
import ke.shulehub.shared.web.NotFoundException;
import ke.shulehub.tenancy.internal.Branch;
import ke.shulehub.tenancy.internal.BranchRepository;

/** Branches of the current tenant. */
@Service
public class BranchDirectory {

    private final BranchRepository branches;
    private final AccessPolicy access;

    public BranchDirectory(BranchRepository branches, AccessPolicy access) {
        this.branches = branches;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<BranchInfo> all() {
        TenantContext.require();
        return branches.findAllByOrderByNameAsc().stream().map(Branch::toInfo).toList();
    }

    @Transactional(readOnly = true)
    public Optional<BranchInfo> find(UUID branchId) {
        return branches.findById(branchId).map(Branch::toInfo);
    }

    /** The branch, or 404 if it doesn't exist in this tenant. */
    @Transactional(readOnly = true)
    public BranchInfo require(UUID branchId) {
        if (branchId == null) {
            throw new NotFoundException("Choose a branch");
        }
        return find(branchId).orElseThrow(() -> new NotFoundException("Branch not found"));
    }

    @Transactional(readOnly = true)
    public Optional<BranchInfo> findByCode(String code) {
        return branches.findByCode(Branch.normalizeCode(code)).map(Branch::toInfo);
    }

    /** Branches by id across tenants (root context); used to label memberships. */
    public Map<UUID, BranchInfo> findAcrossTenants(Collection<UUID> branchIds) {
        if (branchIds.isEmpty()) {
            return Map.of();
        }
        return TenantContext.callAsRoot(() -> branches.findAllById(branchIds)).stream()
                .map(Branch::toInfo)
                .collect(Collectors.toMap(BranchInfo::id, Function.identity()));
    }

    @Transactional
    public BranchInfo create(String name, String code, String county, boolean boardingEnabled) {
        access.requireTenantWide(Permission.BRANCH_MANAGE);
        return createUnchecked(name, code, county, boardingEnabled);
    }

    /** Used by provisioning, which authorises the platform admin itself. */
    @Transactional
    public BranchInfo createUnchecked(String name, String code, String county, boolean boardingEnabled) {
        TenantContext.require();
        Branch branch = new Branch(name, code, county, boardingEnabled);
        if (branch.getCode().isEmpty() || branch.getCode().length() > 6) {
            throw new BadRequestException("Branch code must be 1 to 6 letters or digits");
        }
        if (branches.existsByCode(branch.getCode())) {
            throw new ConflictException("Branch code " + branch.getCode() + " is already used");
        }
        return branches.save(branch).toInfo();
    }
}
