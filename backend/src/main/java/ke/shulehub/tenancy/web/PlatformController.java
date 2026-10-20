package ke.shulehub.tenancy.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ke.shulehub.identity.AccessPolicy;
import ke.shulehub.tenancy.TenantProvisioning;
import ke.shulehub.tenancy.TenantProvisioning.NewBranch;
import ke.shulehub.tenancy.TenantProvisioning.NewDirector;
import ke.shulehub.tenancy.TenantProvisioning.Provisioned;

/** Onboarding a new school group. Shule Hub staff only. */
@RestController
@RequestMapping("/api/platform/tenants")
class PlatformController {

    record BranchRequest(@NotBlank String name, @NotBlank @Size(max = 6) String code, String county,
            boolean boardingEnabled) {
    }

    record DirectorRequest(@NotBlank String givenName, @NotBlank String familyName, @Email String email,
            String phone) {
    }

    record ProvisionRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,62}$", message = "use lower-case letters, digits and dashes") String slug,
            @NotNull @Valid BranchRequest branch,
            @NotNull @Valid DirectorRequest director) {
    }

    private final AccessPolicy access;
    private final TenantProvisioning provisioning;

    PlatformController(AccessPolicy access, TenantProvisioning provisioning) {
        this.access = access;
        this.provisioning = provisioning;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Provisioned provision(@Valid @RequestBody ProvisionRequest request) {
        access.requirePlatformAdmin();
        return provisioning.provision(request.name(), request.slug(),
                new NewBranch(request.branch().name(), request.branch().code(), request.branch().county(),
                        request.branch().boardingEnabled()),
                new NewDirector(request.director().givenName(), request.director().familyName(),
                        request.director().email(), request.director().phone()));
    }
}
