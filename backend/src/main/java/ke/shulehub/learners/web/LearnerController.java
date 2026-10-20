package ke.shulehub.learners.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Size;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ke.shulehub.learners.BoardingStatus;
import ke.shulehub.learners.LearnerDetail;
import ke.shulehub.learners.LearnerStatus;
import ke.shulehub.learners.LearnerSummary;
import ke.shulehub.learners.Learners;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.tenancy.TenantContext;
import ke.shulehub.shared.web.PageResponse;

@RestController
class LearnerController {

    record UpdateLearner(@Size(max = 40) String upi, @Size(max = 40) String assessmentNumber,
            BoardingStatus boardingStatus) {
    }

    private final Learners learners;

    LearnerController(Learners learners) {
        this.learners = learners;
    }

    @GetMapping("/api/learners")
    PageResponse<LearnerSummary> search(@RequestParam UUID branchId,
            @RequestParam(required = false) GradeLevel gradeLevel,
            @RequestParam(required = false) UUID streamId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LearnerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        TenantContext.require();
        return learners.search(new Learners.Search(branchId, gradeLevel, streamId, q, status, page, size));
    }

    @GetMapping("/api/learners/{id}")
    LearnerDetail get(@PathVariable UUID id) {
        TenantContext.require();
        return learners.get(id);
    }

    @PatchMapping("/api/learners/{id}")
    LearnerDetail update(@PathVariable UUID id, @RequestBody UpdateLearner request) {
        TenantContext.require();
        return learners.update(id, request.upi(), request.assessmentNumber(), request.boardingStatus());
    }

    @GetMapping("/api/streams/{id}/learners")
    List<LearnerSummary> roster(@PathVariable UUID id) {
        TenantContext.require();
        return learners.roster(id);
    }
}
