package ke.shulehub.academics.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ke.shulehub.academics.AcademicCalendar;
import ke.shulehub.academics.AcademicCalendar.NewTerm;
import ke.shulehub.academics.AcademicYearInfo;
import ke.shulehub.academics.LearningAreaInfo;
import ke.shulehub.academics.LearningAreas;
import ke.shulehub.academics.StreamInfo;
import ke.shulehub.academics.Streams;
import ke.shulehub.academics.TermInfo;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.school.LevelBand;
import ke.shulehub.shared.tenancy.TenantContext;
import ke.shulehub.shared.web.NotFoundException;

@RestController
class AcademicsController {

    record TermRequest(@Min(1) @Max(3) int number, @NotNull LocalDate startDate, @NotNull LocalDate endDate) {
    }

    record CreateYear(@Min(2000) @Max(2100) int year, @NotNull LocalDate startDate, @NotNull LocalDate endDate,
            @NotEmpty @Size(max = 3) List<@Valid TermRequest> terms) {
    }

    record CreateStream(@NotNull UUID branchId, @NotNull UUID academicYearId, @NotNull GradeLevel gradeLevel,
            @NotBlank @Size(max = 60) String name, @Min(1) @Max(200) int capacity) {
    }

    private final AcademicCalendar calendar;
    private final Streams streams;
    private final LearningAreas learningAreas;

    AcademicsController(AcademicCalendar calendar, Streams streams, LearningAreas learningAreas) {
        this.calendar = calendar;
        this.streams = streams;
        this.learningAreas = learningAreas;
    }

    @GetMapping("/api/academic-years")
    List<AcademicYearInfo> years() {
        TenantContext.require();
        return calendar.years();
    }

    @PostMapping("/api/academic-years")
    @ResponseStatus(HttpStatus.CREATED)
    AcademicYearInfo createYear(@Valid @RequestBody CreateYear request) {
        TenantContext.require();
        return calendar.createYear(request.year(), request.startDate(), request.endDate(),
                request.terms().stream().map(t -> new NewTerm(t.number(), t.startDate(), t.endDate())).toList());
    }

    @GetMapping("/api/terms/current")
    TermInfo currentTerm() {
        TenantContext.require();
        return calendar.currentTerm().orElseThrow(() -> new NotFoundException("No terms have been set up yet"));
    }

    @GetMapping("/api/streams")
    List<StreamInfo> streams(@RequestParam UUID branchId, @RequestParam(required = false) UUID academicYearId) {
        TenantContext.require();
        return streams.list(branchId, academicYearId);
    }

    @PostMapping("/api/streams")
    @ResponseStatus(HttpStatus.CREATED)
    StreamInfo createStream(@Valid @RequestBody CreateStream request) {
        TenantContext.require();
        return streams.create(request.branchId(), request.academicYearId(), request.gradeLevel(), request.name(),
                request.capacity());
    }

    @GetMapping("/api/learning-areas")
    List<LearningAreaInfo> learningAreas(@RequestParam(required = false) LevelBand levelBand) {
        TenantContext.require();
        return learningAreas.list(levelBand);
    }
}
