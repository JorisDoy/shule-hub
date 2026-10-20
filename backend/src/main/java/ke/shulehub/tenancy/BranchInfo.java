package ke.shulehub.tenancy;

import java.util.UUID;

public record BranchInfo(UUID id, String name, String code, String county, boolean boardingEnabled) {
}
