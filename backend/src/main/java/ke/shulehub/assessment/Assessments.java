package ke.shulehub.assessment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.academics.AcademicCalendar;
import ke.shulehub.academics.LearningAreaInfo;
import ke.shulehub.academics.LearningAreas;
import ke.shulehub.academics.StreamInfo;
import ke.shulehub.academics.Streams;
import ke.shulehub.academics.TermInfo;
import ke.shulehub.assessment.GradingScales.ScaleView;
import ke.shulehub.assessment.internal.Assessment;
import ke.shulehub.assessment.internal.AssessmentRepository;
import ke.shulehub.assessment.internal.AssessmentScore;
import ke.shulehub.assessment.internal.AssessmentScoreRepository;
import ke.shulehub.identity.AccessPolicy;
import ke.shulehub.identity.ActorContext;
import ke.shulehub.identity.Permission;
import ke.shulehub.learners.LearnerDirectory;
import ke.shulehub.learners.LearnerSummary;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.web.BadRequestException;
import ke.shulehub.shared.web.NotFoundException;

/** Creating assessments and entering marks or rubric levels. */
@Service
public class Assessments {

    public record AssessmentView(UUID id, String title, AssessmentType type, UUID termId, UUID streamId,
            String streamName, GradeLevel gradeLevel, UUID learningAreaId, String learningAreaName,
            BigDecimal maxScore, BigDecimal weight, LocalDate assessedOn, AssessmentStatus status, int scoredCount,
            int learnerCount) {
    }

    public record ScoreRow(UUID learnerId, String admissionNumber, String learnerName, BigDecimal rawScore,
            String rubricLevel, Integer percent, String bandCode, String comment) {
    }

    public record ScoreSheet(AssessmentView assessment, ScaleView scale, List<ScoreRow> rows) {
    }

    public record ScoreInput(UUID learnerId, BigDecimal rawScore, String rubricLevel, String comment) {
    }

    public record NewAssessment(String title, AssessmentType type, UUID termId, UUID streamId, UUID learningAreaId,
            BigDecimal maxScore, BigDecimal weight, LocalDate assessedOn) {
    }

    private final AssessmentRepository assessments;
    private final AssessmentScoreRepository scores;
    private final Streams streams;
    private final AcademicCalendar calendar;
    private final LearningAreas learningAreas;
    private final LearnerDirectory learners;
    private final GradingScales scales;
    private final AccessPolicy access;

    public Assessments(AssessmentRepository assessments, AssessmentScoreRepository scores, Streams streams,
            AcademicCalendar calendar, LearningAreas learningAreas, LearnerDirectory learners, GradingScales scales,
            AccessPolicy access) {
        this.assessments = assessments;
        this.scores = scores;
        this.streams = streams;
        this.calendar = calendar;
        this.learningAreas = learningAreas;
        this.learners = learners;
        this.scales = scales;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<AssessmentView> list(UUID streamId, UUID termId, UUID learningAreaId) {
        StreamInfo stream = streams.require(streamId);
        requireView(stream);
        List<Assessment> found = termId == null ? assessments.findByStreamIdOrderByAssessedOnDesc(streamId)
                : assessments.findByStreamIdAndTermIdOrderByAssessedOnAscTitleAsc(streamId, termId);
        List<Assessment> filtered = learningAreaId == null ? found
                : found.stream().filter(a -> a.getLearningAreaId().equals(learningAreaId)).toList();
        if (filtered.isEmpty()) {
            return List.of();
        }
        Map<UUID, LearningAreaInfo> areas = learningAreas.findAll(filtered.stream()
                .map(Assessment::getLearningAreaId).distinct().toList());
        int learnerCount = (int) learners.activeCount(streamId);
        Map<UUID, Long> scored = scores.findByAssessmentIdIn(filtered.stream().map(Assessment::getId).toList()).stream()
                .filter(score -> !score.isEmpty())
                .collect(Collectors.groupingBy(AssessmentScore::getAssessmentId, Collectors.counting()));
        return filtered.stream()
                .map(a -> view(a, stream, areas.get(a.getLearningAreaId()), scored.getOrDefault(a.getId(), 0L).intValue(),
                        learnerCount))
                .toList();
    }

    @Transactional
    public AssessmentView create(NewAssessment input) {
        StreamInfo stream = streams.require(input.streamId());
        access.require(Permission.ASSESSMENT_ENTER, stream.branchId(), stream.id());
        TermInfo term = calendar.requireTerm(input.termId());
        if (!term.academicYearId().equals(stream.academicYearId())) {
            throw new BadRequestException("That term is in a different academic year from the class");
        }
        LearningAreaInfo area = learningAreas.require(input.learningAreaId());
        if (area.levelBand() != stream.gradeLevel().band()) {
            throw new BadRequestException(area.name() + " is not taught at " + stream.gradeLevel().label());
        }
        if (input.maxScore() != null && input.maxScore().signum() <= 0) {
            throw new BadRequestException("Maximum score must be greater than zero");
        }
        if (input.maxScore() == null && scales.forBand(stream.gradeLevel().band()).isEmpty()) {
            throw new BadRequestException("Rubric assessments need a grading scale for this level; give a maximum score instead");
        }
        BigDecimal weight = input.weight() == null || input.weight().signum() <= 0 ? BigDecimal.ONE : input.weight();
        Assessment assessment = assessments.save(new Assessment(stream.branchId(), term.id(), stream.id(), area.id(),
                input.title(), input.type(), input.maxScore(), weight, input.assessedOn(),
                ActorContext.current().map(actor -> actor.personId()).orElse(null)));
        return view(assessment, stream, area, 0, (int) learners.activeCount(stream.id()));
    }

    @Transactional(readOnly = true)
    public ScoreSheet sheet(UUID assessmentId) {
        Assessment assessment = load(assessmentId);
        StreamInfo stream = streams.require(assessment.getStreamId());
        requireView(stream);
        return sheet(assessment, stream);
    }

    @Transactional
    public ScoreSheet saveScores(UUID assessmentId, List<ScoreInput> inputs) {
        Assessment assessment = load(assessmentId);
        StreamInfo stream = streams.require(assessment.getStreamId());
        access.require(Permission.ASSESSMENT_ENTER, stream.branchId(), stream.id());
        assessment.requireEditable();
        Optional<ScaleView> scale = scales.forBand(stream.gradeLevel().band());
        Map<UUID, LearnerSummary> roster = learners.inStream(stream.id()).stream()
                .collect(Collectors.toMap(LearnerSummary::id, Function.identity()));
        Map<UUID, AssessmentScore> existing = new HashMap<>();
        scores.findByAssessmentId(assessmentId).forEach(score -> existing.put(score.getLearnerId(), score));
        UUID recordedBy = ActorContext.current().map(actor -> actor.personId()).orElse(null);
        for (ScoreInput input : inputs) {
            if (!roster.containsKey(input.learnerId())) {
                throw new BadRequestException("A learner in the list is not in this class");
            }
            String rubric = null;
            BigDecimal raw = null;
            if (assessment.isRubric()) {
                if (input.rubricLevel() != null && !input.rubricLevel().isBlank()) {
                    rubric = scale.flatMap(s -> Banding.byCode(s.bands(), input.rubricLevel().trim()))
                            .map(Banding.Band::code)
                            .orElseThrow(() -> new BadRequestException("Unknown level " + input.rubricLevel()));
                }
            }
            else if (input.rawScore() != null) {
                if (input.rawScore().signum() < 0 || input.rawScore().compareTo(assessment.getMaxScore()) > 0) {
                    throw new BadRequestException("Scores must be between 0 and " + assessment.getMaxScore().stripTrailingZeros().toPlainString());
                }
                raw = input.rawScore();
            }
            AssessmentScore score = existing.computeIfAbsent(input.learnerId(),
                    learnerId -> scores.save(new AssessmentScore(assessmentId, learnerId)));
            score.record(raw, rubric, input.comment(), recordedBy);
        }
        return sheet(assessment, stream);
    }

    @Transactional
    public AssessmentView release(UUID assessmentId) {
        Assessment assessment = load(assessmentId);
        StreamInfo stream = streams.require(assessment.getStreamId());
        access.require(Permission.ASSESSMENT_MANAGE, stream.branchId());
        assessment.release();
        LearningAreaInfo area = learningAreas.require(assessment.getLearningAreaId());
        int count = (int) learners.activeCount(stream.id());
        int scored = (int) scores.findByAssessmentId(assessmentId).stream().filter(s -> !s.isEmpty()).count();
        return view(assessment, stream, area, scored, count);
    }

    private ScoreSheet sheet(Assessment assessment, StreamInfo stream) {
        Optional<ScaleView> scale = scales.forBand(stream.gradeLevel().band());
        Map<UUID, AssessmentScore> byLearner = scores.findByAssessmentId(assessment.getId()).stream()
                .collect(Collectors.toMap(AssessmentScore::getLearnerId, Function.identity()));
        List<LearnerSummary> roster = learners.inStream(stream.id());
        List<ScoreRow> rows = roster.stream().map(learner -> {
            AssessmentScore score = byLearner.get(learner.id());
            Integer percent = null;
            String band = null;
            if (score != null && score.getRawScore() != null && !assessment.isRubric()) {
                percent = Banding.percent(score.getRawScore(), assessment.getMaxScore());
                int p = percent;
                band = scale.flatMap(s -> Banding.forPercent(s.bands(), p)).map(Banding.Band::code).orElse(null);
            }
            else if (score != null && score.getRubricLevel() != null) {
                band = score.getRubricLevel();
            }
            return new ScoreRow(learner.id(), learner.admissionNumber(), learner.fullName(),
                    score == null ? null : score.getRawScore(), score == null ? null : score.getRubricLevel(), percent,
                    band, score == null ? null : score.getComment());
        }).toList();
        int scored = (int) rows.stream().filter(row -> row.rawScore() != null || row.rubricLevel() != null).count();
        LearningAreaInfo area = learningAreas.require(assessment.getLearningAreaId());
        return new ScoreSheet(view(assessment, stream, area, scored, roster.size()), scale.orElse(null), rows);
    }

    private void requireView(StreamInfo stream) {
        if (!access.has(Permission.ASSESSMENT_ENTER, stream.branchId(), stream.id())
                && !access.has(Permission.ASSESSMENT_MANAGE, stream.branchId())) {
            access.require(Permission.ASSESSMENT_ENTER, stream.branchId(), stream.id());
        }
    }

    private Assessment load(UUID assessmentId) {
        return assessments.findById(assessmentId).orElseThrow(() -> new NotFoundException("Assessment not found"));
    }

    private static AssessmentView view(Assessment a, StreamInfo stream, LearningAreaInfo area, int scored, int learners) {
        return new AssessmentView(a.getId(), a.getTitle(), a.getType(), a.getTermId(), a.getStreamId(), stream.name(),
                stream.gradeLevel(), a.getLearningAreaId(), area == null ? null : area.name(), a.getMaxScore(),
                a.getWeight(), a.getAssessedOn(), a.getStatus(), scored, learners);
    }
}
