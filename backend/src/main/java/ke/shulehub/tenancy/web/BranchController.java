package ke.shulehub.tenancy.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ke.shulehub.shared.tenancy.TenantContext;
import ke.shulehub.tenancy.BranchDirectory;
import ke.shulehub.tenancy.BranchInfo;

@RestController
@RequestMapping("/api/branches")
class BranchController {

    record CreateBranch(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 6) String code,
            @Size(max = 60) String county, boolean boardingEnabled) {
    }

    private final BranchDirectory branches;

    BranchController(BranchDirectory branches) {
        this.branches = branches;
    }

    @GetMapping
    List<BranchInfo> list() {
        return branches.all();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BranchInfo create(@Valid @RequestBody CreateBranch request) {
        TenantContext.require();
        return branches.create(request.name(), request.code(), request.county(), request.boardingEnabled());
    }
}
