package ke.shulehub.learners;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.learners.internal.Enrolment;
import ke.shulehub.learners.internal.EnrolmentRepository;
import ke.shulehub.learners.internal.GuardianLink;
import ke.shulehub.learners.internal.GuardianLinkRepository;
import ke.shulehub.learners.internal.Learner;
import ke.shulehub.learners.internal.LearnerRepository;
import ke.shulehub.learners.internal.LearnerViews;
import ke.shulehub.shared.web.NotFoundException;

/**
 * Learner lookups for other modules. No permission checks here: callers check access for
 * the branch or guardianship they act on.
 */
@Service
public class LearnerDirectory {

    private final LearnerRepository learners;
    private final EnrolmentRepository enrolments;
    private final GuardianLinkRepository guardians;
    private final LearnerViews views;

    public LearnerDirectory(LearnerRepository learners, EnrolmentRepository enrolments,
            GuardianLinkRepository guardians, LearnerViews views) {
        this.learners = learners;
        this.enrolments = enrolments;
        this.guardians = guardians;
        this.views = views;
    }

    @Transactional(readOnly = true)
    public Optional<LearnerSummary> find(UUID learnerId) {
        return learners.findById(learnerId).map(views::summary);
    }

    @Transactional(readOnly = true)
    public LearnerSummary require(UUID learnerId) {
        if (learnerId == null) {
            throw new NotFoundException("Choose a learner");
        }
        return find(learnerId).orElseThrow(() -> new NotFoundException("Learner not found"));
    }

    @Transactional(readOnly = true)
    public Map<UUID, LearnerSummary> findAll(Collection<UUID> learnerIds) {
        if (learnerIds.isEmpty()) {
            return Map.of();
        }
        return views.summaries(learners.findAllById(learnerIds)).stream()
                .collect(Collectors.toMap(LearnerSummary::id, Function.identity()));
    }

    @Transactional(readOnly = true)
    public Optional<LearnerSummary> findByAdmissionNumber(String accountReference) {
        String normalized = Learner.normalizeAdmissionNumber(accountReference);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return learners.findByAdmissionNumber(normalized).map(views::summary);
    }

    @Transactional(readOnly = true)
    public List<LearnerSummary> activeInBranch(UUID branchId) {
        return views.summaries(learners.findByBranchIdAndStatus(branchId, LearnerStatus.ACTIVE));
    }

    /** Learners actively enrolled in a stream, by family then given name. */
    @Transactional(readOnly = true)
    public List<LearnerSummary> inStream(UUID streamId) {
        List<UUID> ids = enrolments.findByStreamIdAndStatus(streamId, Enrolment.ACTIVE).stream()
                .map(Enrolment::getLearnerId)
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return views.summaries(learners.findAllById(ids)).stream()
                .sorted(Comparator.comparing(LearnerSummary::familyName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(LearnerSummary::givenName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<UUID> streamOf(UUID learnerId, UUID academicYearId) {
        return enrolments.findByLearnerIdAndAcademicYearIdAndStatus(learnerId, academicYearId, Enrolment.ACTIVE)
                .map(Enrolment::getStreamId);
    }

    @Transactional(readOnly = true)
    public long activeCount(UUID streamId) {
        return enrolments.countByStreamIdAndStatus(streamId, Enrolment.ACTIVE);
    }

    @Transactional(readOnly = true)
    public boolean isGuardian(UUID personId, UUID learnerId) {
        return personId != null && guardians.existsByPersonIdAndLearnerId(personId, learnerId);
    }

    @Transactional(readOnly = true)
    public List<LearnerSummary> childrenOf(UUID personId) {
        List<UUID> ids = guardians.findByPersonId(personId).stream().map(GuardianLink::getLearnerId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return views.summaries(learners.findAllById(ids)).stream()
                .sorted(Comparator.comparing(LearnerSummary::givenName))
                .toList();
    }
}
