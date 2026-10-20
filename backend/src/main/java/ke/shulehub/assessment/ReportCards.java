package ke.shulehub.assessment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
import ke.shulehub.assessment.internal.ReportCard;
import ke.shulehub.assessment.internal.ReportCardRepository;
import ke.shulehub.identity.AccessPolicy;
import ke.shulehub.identity.ActorContext;
import ke.shulehub.identity.Permission;
import ke.shulehub.learners.LearnerDirectory;
import ke.shulehub.learners.LearnerSummary;
import ke.shulehub.shared.web.BadRequestException;
import ke.shulehub.shared.web.NotFoundException;

/**
 * Term report cards. Each learning area's result is the weighted mean percentage of the term's
 * marked assessments, banded on the level's scale; rubric-only areas use the mean band points.
 * Cards move DRAFT → APPROVED → RELEASED, and parents only see released cards.
 */
@Service
public class ReportCards {

    public record LearnerRef(UUID id, String fullName, String admissionNumber, String gradeLevel, String streamName) {
    }

    public record TermRef(UUID id, int year, int number) {
    }

    public record ScaleRef(String code, String name) {
    }

    public record AreaResult(UUID learningAreaId, String name, Integer percent, String bandCode, String bandLabel,
            Integer points, int assessmentCount, String comment) {
    }

    public record ReportCardView(LearnerRef learner, TermRef term, ScaleRef scale, List<AreaResult> areas,
            String classTeacherComment, String headComment, ReportCardStatus status, Instant releasedAt) {
    }

    public record CardSummary(UUID learnerId, String learnerName, String admissionNumber, ReportCardStatus status) {
    }

    public record ReleasedTerm(UUID termId, int year, int number, Instant releasedAt) {
    }

    public record AreaComment(UUID learningAreaId, String comment) {
    }

    private final ReportCardRepository cards;
    private final AssessmentRepository assessments;
    private final AssessmentScoreRepository scores;
    private final LearnerDirectory learners;
    private final Streams streams;
    private final AcademicCalendar calendar;
    private final LearningAreas learningAreas;
    private final GradingScales scales;
    private final AccessPolicy access;
    private final Clock clock;

    public ReportCards(ReportCardRepository cards, AssessmentRepository assessments, AssessmentScoreRepository scores,
            LearnerDirectory learners, Streams streams, AcademicCalendar calendar, LearningAreas learningAreas,
            GradingScales scales, AccessPolicy access, Clock clock) {
        this.cards = cards;
        this.assessments = assessments;
        this.scores = scores;
        this.learners = learners;
        this.streams = streams;
        this.calendar = calendar;
        this.learningAreas = learningAreas;
        this.scales = scales;
        this.access = access;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CardSummary> list(UUID streamId, UUID termId) {
        StreamInfo stream = streams.require(streamId);
        requireView(stream);
        calendar.requireTerm(termId);
        Map<UUID, ReportCard> byLearner = cards.findByStreamIdAndTermId(streamId, termId).stream()
                .collect(Collectors.toMap(ReportCard::getLearnerId, Function.identity()));
        return learners.inStream(streamId).stream()
                .map(learner -> new CardSummary(learner.id(), learner.fullName(), learner.admissionNumber(),
                        Optional.ofNullable(byLearner.get(learner.id())).map(ReportCard::getStatus)
                                .orElse(ReportCardStatus.DRAFT)))
                .toList();
    }

    /** The card for staff, in any status. */
    @Transactional(readOnly = true)
    public ReportCardView forStaff(UUID learnerId, UUID termId) {
        Context context = context(learnerId, termId);
        requireView(context.stream());
        return build(context);
    }

    /** The card for a guardian; only released cards exist from their point of view. */
    @Transactional(readOnly = true)
    public ReportCardView released(UUID learnerId, UUID termId) {
        ReportCard card = cards.findByLearnerIdAndTermId(learnerId, termId)
                .filter(found -> found.getStatus() == ReportCardStatus.RELEASED)
                .orElseThrow(() -> new NotFoundException("This report card has not been released yet"));
        return build(context(learnerId, termId, card.getStreamId()));
    }

    @Transactional(readOnly = true)
    public List<ReleasedTerm> releasedTerms(UUID learnerId) {
        return cards.findByLearnerIdAndStatus(learnerId, ReportCardStatus.RELEASED).stream()
                .map(card -> {
                    TermInfo term = calendar.requireTerm(card.getTermId());
                    return new ReleasedTerm(term.id(), term.year(), term.number(), card.getReleasedAt());
                })
                .sorted(Comparator.comparing(ReleasedTerm::year).thenComparing(ReleasedTerm::number).reversed())
                .toList();
    }

    @Transactional
    public ReportCardView updateComments(UUID learnerId, UUID termId, String classTeacherComment, String headComment,
            List<AreaComment> areaComments) {
        Context context = context(learnerId, termId);
        StreamInfo stream = context.stream();
        ReportCard card = context.card().orElseGet(() -> cards.save(
                new ReportCard(stream.branchId(), learnerId, termId, stream.id())));
        if (classTeacherComment != null || (areaComments != null && !areaComments.isEmpty())) {
            access.require(Permission.ASSESSMENT_ENTER, stream.branchId(), stream.id());
        }
        if (classTeacherComment != null) {
            card.setClassTeacherComment(classTeacherComment);
        }
        if (headComment != null) {
            access.require(Permission.REPORTS_RELEASE, stream.branchId());
            card.setHeadComment(headComment);
        }
        if (areaComments != null) {
            for (AreaComment comment : areaComments) {
                card.setAreaComment(comment.learningAreaId(), comment.comment());
            }
        }
        return build(new Context(context.learner(), context.term(), stream, Optional.of(card)));
    }

    @Transactional
    public ReportCardView approve(UUID learnerId, UUID termId) {
        Context context = context(learnerId, termId);
        StreamInfo stream = context.stream();
        access.require(Permission.ASSESSMENT_MANAGE, stream.branchId());
        ReportCard card = context.card().orElseGet(() -> cards.save(
                new ReportCard(stream.branchId(), learnerId, termId, stream.id())));
        card.approve(ActorContext.requirePerson().id(), clock.instant());
        return build(new Context(context.learner(), context.term(), stream, Optional.of(card)));
    }

    /** Releases every approved card in the stream for the term. */
    @Transactional
    public int release(UUID streamId, UUID termId) {
        StreamInfo stream = streams.require(streamId);
        access.require(Permission.REPORTS_RELEASE, stream.branchId());
        UUID personId = ActorContext.requirePerson().id();
        Instant now = clock.instant();
        int released = 0;
        for (ReportCard card : cards.findByStreamIdAndTermId(streamId, termId)) {
            if (card.getStatus() == ReportCardStatus.APPROVED) {
                card.release(personId, now);
                released++;
            }
        }
        return released;
    }

    private record Context(LearnerSummary learner, TermInfo term, StreamInfo stream, Optional<ReportCard> card) {
    }

    private Context context(UUID learnerId, UUID termId) {
        LearnerSummary learner = learners.require(learnerId);
        TermInfo term = calendar.requireTerm(termId);
        Optional<ReportCard> card = cards.findByLearnerIdAndTermId(learnerId, termId);
        UUID streamId = card.map(ReportCard::getStreamId)
                .or(() -> learners.streamOf(learnerId, term.academicYearId()))
                .orElseThrow(() -> new BadRequestException("This learner was not in a class during that term"));
        return new Context(learner, term, streams.require(streamId), card);
    }

    private Context context(UUID learnerId, UUID termId, UUID streamId) {
        return new Context(learners.require(learnerId), calendar.requireTerm(termId), streams.require(streamId),
                cards.findByLearnerIdAndTermId(learnerId, termId));
    }

    private ReportCardView build(Context context) {
        StreamInfo stream = context.stream();
        TermInfo term = context.term();
        LearnerSummary learner = context.learner();
        Optional<ScaleView> scale = scales.forBand(stream.gradeLevel().band());
        List<Assessment> termAssessments = assessments.findByStreamIdAndTermIdOrderByAssessedOnAscTitleAsc(stream.id(),
                term.id());
        Map<UUID, AssessmentScore> learnerScores = termAssessments.isEmpty() ? Map.of()
                : scores.findByAssessmentIdInAndLearnerId(termAssessments.stream().map(Assessment::getId).toList(),
                        learner.id()).stream()
                        .collect(Collectors.toMap(AssessmentScore::getAssessmentId, Function.identity()));
        List<AreaResult> areas = new ArrayList<>();
        for (LearningAreaInfo area : learningAreas.list(stream.gradeLevel().band())) {
            List<Assessment> forArea = termAssessments.stream()
                    .filter(a -> a.getLearningAreaId().equals(area.id())).toList();
            AreaResult result = areaResult(area, forArea, learnerScores, scale);
            String comment = context.card().flatMap(card -> card.areaComment(area.id())).orElse(null);
            areas.add(new AreaResult(result.learningAreaId(), result.name(), result.percent(), result.bandCode(),
                    result.bandLabel(), result.points(), result.assessmentCount(), comment));
        }
        ReportCard card = context.card().orElse(null);
        return new ReportCardView(
                new LearnerRef(learner.id(), learner.fullName(), learner.admissionNumber(),
                        stream.gradeLevel().name(), stream.name()),
                new TermRef(term.id(), term.year(), term.number()),
                scale.map(s -> new ScaleRef(s.code(), s.name())).orElse(null),
                areas,
                card == null ? null : card.getClassTeacherComment(),
                card == null ? null : card.getHeadComment(),
                card == null ? ReportCardStatus.DRAFT : card.getStatus(),
                card == null ? null : card.getReleasedAt());
    }

    /** Weighted mean of marked assessments; otherwise mean rubric points; otherwise no result. */
    static AreaResult areaResult(LearningAreaInfo area, List<Assessment> forArea, Map<UUID, AssessmentScore> scoresById,
            Optional<ScaleView> scale) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal weightTotal = BigDecimal.ZERO;
        int rubricPoints = 0;
        int rubricCount = 0;
        int counted = 0;
        for (Assessment assessment : forArea) {
            AssessmentScore score = scoresById.get(assessment.getId());
            if (score == null || score.isEmpty()) {
                continue;
            }
            if (!assessment.isRubric() && score.getRawScore() != null) {
                BigDecimal percent = score.getRawScore().multiply(BigDecimal.valueOf(100))
                        .divide(assessment.getMaxScore(), 4, RoundingMode.HALF_UP);
                weightedSum = weightedSum.add(percent.multiply(assessment.getWeight()));
                weightTotal = weightTotal.add(assessment.getWeight());
                counted++;
            }
            else if (assessment.isRubric() && score.getRubricLevel() != null && scale.isPresent()) {
                Optional<Banding.Band> band = Banding.byCode(scale.get().bands(), score.getRubricLevel());
                if (band.isPresent()) {
                    rubricPoints += band.get().points();
                    rubricCount++;
                    counted++;
                }
            }
        }
        Integer percent = null;
        Optional<Banding.Band> band = Optional.empty();
        if (weightTotal.signum() > 0) {
            percent = Banding.roundPercent(weightedSum.divide(weightTotal, 4, RoundingMode.HALF_UP));
            int p = percent;
            band = scale.flatMap(s -> Banding.forPercent(s.bands(), p));
        }
        else if (rubricCount > 0) {
            int meanPoints = BigDecimal.valueOf(rubricPoints)
                    .divide(BigDecimal.valueOf(rubricCount), 0, RoundingMode.HALF_UP).intValue();
            band = scale.flatMap(s -> Banding.forPoints(s.bands(), meanPoints));
        }
        return new AreaResult(area.id(), area.name(), percent, band.map(Banding.Band::code).orElse(null),
                band.map(Banding.Band::label).orElse(null), band.map(Banding.Band::points).orElse(null), counted, null);
    }

    private void requireView(StreamInfo stream) {
        if (!access.has(Permission.ASSESSMENT_ENTER, stream.branchId(), stream.id())
                && !access.has(Permission.ASSESSMENT_MANAGE, stream.branchId())) {
            access.require(Permission.ASSESSMENT_ENTER, stream.branchId(), stream.id());
        }
    }
}
