package ke.shulehub.assessment.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ke.shulehub.assessment.AssessmentType;
import ke.shulehub.assessment.Assessments;
import ke.shulehub.assessment.Assessments.AssessmentView;
import ke.shulehub.assessment.Assessments.ScoreSheet;
import ke.shulehub.assessment.GradingScales;
import ke.shulehub.assessment.GradingScales.ScaleView;
import ke.shulehub.assessment.ReportCards;
import ke.shulehub.assessment.ReportCards.CardSummary;
import ke.shulehub.assessment.ReportCards.ReportCardView;
import ke.shulehub.shared.tenancy.TenantContext;

@RestController
class AssessmentController {

    record CreateAssessment(@NotBlank @Size(max = 120) String title, @NotNull AssessmentType type,
            @NotNull UUID termId, @NotNull UUID streamId, @NotNull UUID learningAreaId,
            @DecimalMin("0.01") BigDecimal maxScore, @DecimalMin("0.01") BigDecimal weight,
            @NotNull LocalDate assessedOn) {
    }

    record ScoreRequest(@NotNull UUID learnerId, @DecimalMin("0") BigDecimal rawScore, @Size(max = 10) String rubricLevel,
            @Size(max = 500) String comment) {
    }

    record SaveScores(@NotNull List<@Valid ScoreRequest> scores) {
    }

    record AreaCommentRequest(@NotNull UUID learningAreaId, @Size(max = 500) String comment) {
    }

    record UpdateCard(@Size(max = 1000) String classTeacherComment, @Size(max = 1000) String headComment,
            List<@Valid AreaCommentRequest> areaComments) {
    }

    record ReleaseRequest(@NotNull UUID streamId, @NotNull UUID termId) {
    }

    private final GradingScales scales;
    private final Assessments assessments;
    private final ReportCards reportCards;

    AssessmentController(GradingScales scales, Assessments assessments, ReportCards reportCards) {
        this.scales = scales;
        this.assessments = assessments;
        this.reportCards = reportCards;
    }

    @GetMapping("/api/assessment/grading-scales")
    List<ScaleView> gradingScales() {
        TenantContext.require();
        return scales.all();
    }

    @GetMapping("/api/assessment/assessments")
    List<AssessmentView> list(@RequestParam UUID streamId, @RequestParam(required = false) UUID termId,
            @RequestParam(required = false) UUID learningAreaId) {
        TenantContext.require();
        return assessments.list(streamId, termId, learningAreaId);
    }

    @PostMapping("/api/assessment/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    AssessmentView create(@Valid @RequestBody CreateAssessment request) {
        TenantContext.require();
        return assessments.create(new Assessments.NewAssessment(request.title(), request.type(), request.termId(),
                request.streamId(), request.learningAreaId(), request.maxScore(), request.weight(),
                request.assessedOn()));
    }

    @GetMapping("/api/assessment/assessments/{id}/scores")
    ScoreSheet scores(@PathVariable UUID id) {
        TenantContext.require();
        return assessments.sheet(id);
    }

    @PutMapping("/api/assessment/assessments/{id}/scores")
    ScoreSheet saveScores(@PathVariable UUID id, @Valid @RequestBody SaveScores request) {
        TenantContext.require();
        return assessments.saveScores(id, request.scores().stream()
                .map(score -> new Assessments.ScoreInput(score.learnerId(), score.rawScore(), score.rubricLevel(),
                        score.comment()))
                .toList());
    }

    @PostMapping("/api/assessment/assessments/{id}/release")
    AssessmentView release(@PathVariable UUID id) {
        TenantContext.require();
        return assessments.release(id);
    }

    @GetMapping("/api/assessment/report-cards")
    List<CardSummary> reportCards(@RequestParam UUID streamId, @RequestParam UUID termId) {
        TenantContext.require();
        return reportCards.list(streamId, termId);
    }

    @GetMapping("/api/assessment/report-cards/{learnerId}/{termId}")
    ReportCardView reportCard(@PathVariable UUID learnerId, @PathVariable UUID termId) {
        TenantContext.require();
        return reportCards.forStaff(learnerId, termId);
    }

    @PutMapping("/api/assessment/report-cards/{learnerId}/{termId}")
    ReportCardView updateReportCard(@PathVariable UUID learnerId, @PathVariable UUID termId,
            @Valid @RequestBody UpdateCard request) {
        TenantContext.require();
        List<ReportCards.AreaComment> areaComments = request.areaComments() == null ? List.of()
                : request.areaComments().stream()
                        .map(c -> new ReportCards.AreaComment(c.learningAreaId(), c.comment()))
                        .toList();
        return reportCards.updateComments(learnerId, termId, request.classTeacherComment(), request.headComment(),
                areaComments);
    }

    @PostMapping("/api/assessment/report-cards/{learnerId}/{termId}/approve")
    ReportCardView approve(@PathVariable UUID learnerId, @PathVariable UUID termId) {
        TenantContext.require();
        return reportCards.approve(learnerId, termId);
    }

    @PostMapping("/api/assessment/report-cards/release")
    Map<String, Integer> release(@Valid @RequestBody ReleaseRequest request) {
        TenantContext.require();
        return Map.of("released", reportCards.release(request.streamId(), request.termId()));
    }
}
