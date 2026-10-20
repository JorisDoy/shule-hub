package ke.shulehub.admissions.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ke.shulehub.admissions.Admissions;
import ke.shulehub.admissions.Admissions.AdmissionResult;
import ke.shulehub.admissions.Admissions.ApplicationForm;
import ke.shulehub.admissions.Admissions.Detail;
import ke.shulehub.admissions.Admissions.GuardianDetails;
import ke.shulehub.admissions.Admissions.LearnerDetails;
import ke.shulehub.admissions.Admissions.Summary;
import ke.shulehub.admissions.ApplicationStatus;
import ke.shulehub.learners.Gender;
import ke.shulehub.learners.Relationship;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.tenancy.TenantContext;

@RestController
class AdmissionsController {

    record LearnerRequest(@NotBlank @Size(max = 60) String givenName, @NotBlank @Size(max = 60) String familyName,
            @Past LocalDate dateOfBirth, Gender gender) {
    }

    record GuardianRequest(@NotBlank @Size(max = 60) String givenName, @NotBlank @Size(max = 60) String familyName,
            @NotBlank String phone, @Email String email, @NotNull Relationship relationship) {
    }

    record EnquiryRequest(@NotBlank String branchCode, @NotNull GradeLevel gradeLevel, boolean boardingRequested,
            @NotNull @Valid LearnerRequest learner, @NotNull @Valid GuardianRequest guardian,
            @Size(max = 1000) String notes) {
    }

    record ApplicationRequest(@NotNull UUID branchId, UUID academicYearId, @NotNull GradeLevel gradeLevel,
            boolean boardingRequested, @NotNull @Valid LearnerRequest learner,
            @NotNull @Valid GuardianRequest guardian, @Size(max = 1000) String notes) {
    }

    record TransitionRequest(@NotNull ApplicationStatus to, @Size(max = 1000) String note, LocalDate assessmentDate,
            LocalDate offerExpiresOn) {
    }

    record AdmitRequest(@NotNull UUID streamId) {
    }

    private final Admissions admissions;

    AdmissionsController(Admissions admissions) {
        this.admissions = admissions;
    }

    @PostMapping("/api/public/{tenantSlug}/enquiries")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, String> enquire(@PathVariable String tenantSlug, @Valid @RequestBody EnquiryRequest request) {
        String reference = admissions.submitEnquiry(tenantSlug, request.branchCode(),
                new ApplicationForm(null, null, request.gradeLevel(), request.boardingRequested(),
                        learner(request.learner()), guardian(request.guardian()), request.notes()));
        return Map.of("reference", reference);
    }

    @GetMapping("/api/admissions/applications")
    List<Summary> list(@RequestParam UUID branchId, @RequestParam(required = false) ApplicationStatus status) {
        TenantContext.require();
        return admissions.list(branchId, status);
    }

    @PostMapping("/api/admissions/applications")
    @ResponseStatus(HttpStatus.CREATED)
    Detail create(@Valid @RequestBody ApplicationRequest request) {
        TenantContext.require();
        return admissions.create(new ApplicationForm(request.branchId(), request.academicYearId(), request.gradeLevel(),
                request.boardingRequested(), learner(request.learner()), guardian(request.guardian()),
                request.notes()));
    }

    @GetMapping("/api/admissions/applications/{id}")
    Detail get(@PathVariable UUID id) {
        TenantContext.require();
        return admissions.get(id);
    }

    @PostMapping("/api/admissions/applications/{id}/transitions")
    Detail transition(@PathVariable UUID id, @Valid @RequestBody TransitionRequest request) {
        TenantContext.require();
        return admissions.transition(id, request.to(), request.note(), request.assessmentDate(),
                request.offerExpiresOn());
    }

    @PostMapping("/api/admissions/applications/{id}/admit")
    AdmissionResult admit(@PathVariable UUID id, @Valid @RequestBody AdmitRequest request) {
        TenantContext.require();
        return admissions.admit(id, request.streamId());
    }

    private static LearnerDetails learner(LearnerRequest request) {
        return new LearnerDetails(request.givenName(), request.familyName(), request.dateOfBirth(), request.gender());
    }

    private static GuardianDetails guardian(GuardianRequest request) {
        return new GuardianDetails(request.givenName(), request.familyName(), request.phone(), request.email(),
                request.relationship());
    }
}
