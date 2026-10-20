package ke.shulehub.learners;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.academics.StreamInfo;
import ke.shulehub.academics.Streams;
import ke.shulehub.identity.AccessPolicy;
import ke.shulehub.identity.Grant;
import ke.shulehub.identity.Permission;
import ke.shulehub.identity.PersonDirectory;
import ke.shulehub.identity.PersonRef;
import ke.shulehub.identity.Role;
import ke.shulehub.identity.RoleAssignments;
import ke.shulehub.learners.internal.Enrolment;
import ke.shulehub.learners.internal.EnrolmentRepository;
import ke.shulehub.learners.internal.GuardianLink;
import ke.shulehub.learners.internal.GuardianLinkRepository;
import ke.shulehub.learners.internal.Learner;
import ke.shulehub.learners.internal.LearnerRepository;
import ke.shulehub.learners.internal.LearnerViews;
import ke.shulehub.shared.persistence.SequenceService;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.web.BadRequestException;
import ke.shulehub.shared.web.ConflictException;
import ke.shulehub.shared.web.NotFoundException;
import ke.shulehub.shared.web.PageResponse;
import ke.shulehub.tenancy.BranchDirectory;
import ke.shulehub.tenancy.BranchInfo;

/** Learner records: admission into a stream, search, and profile updates. */
@Service
public class Learners {

    public record GuardianDetails(String givenName, String familyName, String phone, String email,
            Relationship relationship) {
    }

    public record AdmitCommand(UUID branchId, UUID streamId, String givenName, String familyName, String otherNames,
            LocalDate dateOfBirth, Gender gender, GradeLevel gradeLevel, BoardingStatus boardingStatus,
            GuardianDetails guardian) {
    }

    public record Admitted(UUID learnerId, String admissionNumber, UUID guardianPersonId) {
    }

    public record Search(UUID branchId, GradeLevel gradeLevel, UUID streamId, String query, LearnerStatus status,
            int page, int size) {
    }

    private final LearnerRepository learners;
    private final GuardianLinkRepository guardians;
    private final EnrolmentRepository enrolments;
    private final LearnerViews views;
    private final Streams streams;
    private final BranchDirectory branches;
    private final PersonDirectory people;
    private final RoleAssignments roles;
    private final SequenceService sequences;
    private final AccessPolicy access;
    private final Clock clock;

    public Learners(LearnerRepository learners, GuardianLinkRepository guardians, EnrolmentRepository enrolments,
            LearnerViews views, Streams streams, BranchDirectory branches, PersonDirectory people,
            RoleAssignments roles, SequenceService sequences, AccessPolicy access, Clock clock) {
        this.learners = learners;
        this.guardians = guardians;
        this.enrolments = enrolments;
        this.views = views;
        this.streams = streams;
        this.branches = branches;
        this.people = people;
        this.roles = roles;
        this.sequences = sequences;
        this.access = access;
        this.clock = clock;
    }

    /**
     * Admits a learner into a stream: issues the admission number (branch code + sequence, e.g.
     * RUA0042, which parents also use as the M-Pesa account number), links the guardian and gives
     * them parent access. Callers authorise (admissions requires ADMISSIONS_MANAGE).
     */
    @Transactional
    public Admitted admit(AdmitCommand command) {
        BranchInfo branch = branches.require(command.branchId());
        StreamInfo stream = streams.require(command.streamId());
        if (!stream.branchId().equals(branch.id())) {
            throw new BadRequestException("That class belongs to another branch");
        }
        if (stream.gradeLevel() != command.gradeLevel()) {
            throw new BadRequestException("That class is " + stream.gradeLevel().label() + ", but the learner is joining "
                    + command.gradeLevel().label());
        }
        if (command.boardingStatus() == BoardingStatus.BOARDING && !branch.boardingEnabled()) {
            throw new BadRequestException(branch.name() + " does not offer boarding");
        }
        if (enrolments.countByStreamIdAndStatus(stream.id(), Enrolment.ACTIVE) >= stream.capacity()) {
            throw new ConflictException(stream.name() + " is full (" + stream.capacity() + " learners)");
        }
        long sequence = sequences.next("ADM-" + branch.code());
        String admissionNumber = branch.code() + String.format(Locale.ROOT, "%04d", sequence);
        LocalDate today = LocalDate.now(clock);
        Learner learner = learners.save(new Learner(branch.id(), admissionNumber, command.givenName(),
                command.familyName(), command.otherNames(), command.dateOfBirth(), command.gender(),
                command.gradeLevel(), command.boardingStatus(), today));

        GuardianDetails guardian = command.guardian();
        PersonRef guardianPerson = people.findOrCreate(guardian.givenName(), guardian.familyName(), guardian.phone(),
                guardian.email());
        guardians.save(new GuardianLink(learner.getId(), guardianPerson.id(), guardian.relationship(), true, true, true));
        roles.assign(guardianPerson.id(), Grant.group(Role.PARENT));
        enrolments.save(new Enrolment(learner.getId(), stream.id(), stream.academicYearId(), today));
        return new Admitted(learner.getId(), learner.getAdmissionNumber(), guardianPerson.id());
    }

    @Transactional(readOnly = true)
    public PageResponse<LearnerSummary> search(Search search) {
        BranchInfo branch = branches.require(search.branchId());
        access.require(Permission.LEARNERS_VIEW, branch.id(), search.streamId());
        int size = Math.clamp(search.size(), 1, 100);
        PageRequest pageRequest = PageRequest.of(Math.max(search.page(), 0), size,
                Sort.by("familyName", "givenName"));
        Page<Learner> page = learners.findAll(matching(search), pageRequest);
        List<LearnerSummary> summaries = views.summaries(page.getContent());
        return new PageResponse<>(summaries, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public LearnerDetail get(UUID learnerId) {
        Learner learner = load(learnerId);
        LearnerDetail detail = views.detail(learner);
        access.require(Permission.LEARNERS_VIEW, learner.getBranchId(), detail.streamId());
        return detail;
    }

    @Transactional
    public LearnerDetail update(UUID learnerId, String upi, String assessmentNumber, BoardingStatus boardingStatus) {
        Learner learner = load(learnerId);
        access.require(Permission.LEARNERS_EDIT, learner.getBranchId());
        learner.updateIdentifiers(upi, assessmentNumber);
        if (boardingStatus != null) {
            if (boardingStatus == BoardingStatus.BOARDING && !branches.require(learner.getBranchId()).boardingEnabled()) {
                throw new BadRequestException("This branch does not offer boarding");
            }
            learner.changeBoardingStatus(boardingStatus);
        }
        return views.detail(learner);
    }

    @Transactional(readOnly = true)
    public List<LearnerSummary> roster(UUID streamId) {
        StreamInfo stream = streams.require(streamId);
        access.require(Permission.LEARNERS_VIEW, stream.branchId(), stream.id());
        List<UUID> ids = enrolments.findByStreamIdAndStatus(streamId, Enrolment.ACTIVE).stream()
                .map(Enrolment::getLearnerId).toList();
        return views.summaries(learners.findAllById(ids)).stream()
                .sorted(Comparator.comparing(LearnerSummary::familyName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(LearnerSummary::givenName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Learner load(UUID learnerId) {
        return learners.findById(learnerId).orElseThrow(() -> new NotFoundException("Learner not found"));
    }

    private static Specification<Learner> matching(Search search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("branchId"), search.branchId()));
            if (search.gradeLevel() != null) {
                predicates.add(cb.equal(root.get("gradeLevel"), search.gradeLevel()));
            }
            predicates.add(cb.equal(root.get("status"), search.status() == null ? LearnerStatus.ACTIVE : search.status()));
            if (search.query() != null && !search.query().isBlank()) {
                String like = "%" + search.query().trim().toLowerCase(Locale.ROOT) + "%";
                List<Predicate> matches = new ArrayList<>();
                matches.add(cb.like(cb.lower(root.<String>get("givenName")), like));
                matches.add(cb.like(cb.lower(root.<String>get("familyName")), like));
                String admission = Learner.normalizeAdmissionNumber(search.query());
                if (!admission.isEmpty()) {
                    matches.add(cb.like(root.<String>get("admissionNumber"), "%" + admission + "%"));
                }
                predicates.add(cb.or(matches.toArray(Predicate[]::new)));
            }
            if (search.streamId() != null && query != null) {
                Subquery<UUID> enrolled = query.subquery(UUID.class);
                var enrolment = enrolled.from(Enrolment.class);
                enrolled.select(enrolment.<UUID>get("learnerId")).where(
                        cb.equal(enrolment.get("streamId"), search.streamId()),
                        cb.equal(enrolment.get("status"), Enrolment.ACTIVE));
                predicates.add(root.get("id").in(enrolled));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
