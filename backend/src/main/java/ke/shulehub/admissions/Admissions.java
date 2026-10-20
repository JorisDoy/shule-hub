package ke.shulehub.admissions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ke.shulehub.admissions.internal.AdmissionApplication;
import ke.shulehub.admissions.internal.AdmissionApplication.Source;
import ke.shulehub.admissions.internal.AdmissionApplicationRepository;
import ke.shulehub.admissions.internal.AdmissionEvent;
import ke.shulehub.admissions.internal.AdmissionEventRepository;
import ke.shulehub.academics.AcademicCalendar;
import ke.shulehub.identity.AccessPolicy;
import ke.shulehub.identity.ActorContext;
import ke.shulehub.identity.Actor;
import ke.shulehub.identity.Permission;
import ke.shulehub.identity.PersonDirectory;
import ke.shulehub.identity.PersonRef;
import ke.shulehub.learners.BoardingStatus;
import ke.shulehub.learners.Gender;
import ke.shulehub.learners.LearnerDirectory;
import ke.shulehub.learners.LearnerSummary;
import ke.shulehub.learners.Learners;
import ke.shulehub.learners.Relationship;
import ke.shulehub.shared.contact.PhoneNumbers;
import ke.shulehub.shared.persistence.SequenceService;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.tenancy.TenantContext;
import ke.shulehub.shared.web.BadRequestException;
import ke.shulehub.shared.web.ConflictException;
import ke.shulehub.shared.web.NotFoundException;
import ke.shulehub.tenancy.BranchDirectory;
import ke.shulehub.tenancy.BranchInfo;
import ke.shulehub.tenancy.TenantDirectory;
import ke.shulehub.tenancy.TenantInfo;

/** Enquiry → application → offer → admission. */
@Service
public class Admissions {

    public record LearnerDetails(String givenName, String familyName, LocalDate dateOfBirth, Gender gender) {
    }

    public record GuardianDetails(String givenName, String familyName, String phone, String email,
            Relationship relationship) {
    }

    public record ApplicationForm(UUID branchId, UUID academicYearId, GradeLevel gradeLevel, boolean boardingRequested,
            LearnerDetails learner, GuardianDetails guardian, String notes) {
    }

    public record Summary(UUID id, String reference, UUID branchId, String learnerName, GradeLevel gradeLevel,
            ApplicationStatus status, String guardianName, String guardianPhone, Source source, Instant createdAt) {
    }

    public record HistoryEntry(ApplicationStatus fromStatus, ApplicationStatus toStatus, String note, String actorName,
            Instant occurredAt) {
    }

    public record Detail(UUID id, String reference, UUID branchId, String learnerName, GradeLevel gradeLevel,
            ApplicationStatus status, String guardianName, String guardianPhone, Source source, Instant createdAt,
            UUID academicYearId, LearnerDetails learner, GuardianDetails guardian, boolean boardingRequested,
            String notes, LocalDate assessmentDate, LocalDate offerExpiresOn, UUID learnerId, String admissionNumber,
            List<ApplicationStatus> allowedTransitions, List<HistoryEntry> history) {
    }

    public record AdmissionResult(UUID learnerId, String admissionNumber) {
    }

    private final AdmissionApplicationRepository applications;
    private final AdmissionEventRepository events;
    private final BranchDirectory branches;
    private final TenantDirectory tenants;
    private final AcademicCalendar calendar;
    private final Learners learners;
    private final LearnerDirectory learnerDirectory;
    private final PersonDirectory people;
    private final SequenceService sequences;
    private final AccessPolicy access;
    private final Clock clock;
    private final TransactionTemplate transactions;

    public Admissions(AdmissionApplicationRepository applications, AdmissionEventRepository events,
            BranchDirectory branches, TenantDirectory tenants, AcademicCalendar calendar, Learners learners,
            LearnerDirectory learnerDirectory, PersonDirectory people, SequenceService sequences, AccessPolicy access, Clock clock,
            PlatformTransactionManager transactionManager) {
        this.applications = applications;
        this.events = events;
        this.branches = branches;
        this.tenants = tenants;
        this.calendar = calendar;
        this.learners = learners;
        this.learnerDirectory = learnerDirectory;
        this.people = people;
        this.sequences = sequences;
        this.access = access;
        this.clock = clock;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /** A parent's enquiry from the school's public page. No login; the tenant comes from the URL. */
    public String submitEnquiry(String tenantSlug, String branchCode, ApplicationForm form) {
        TenantInfo tenant = tenants.findBySlug(tenantSlug).orElseThrow(() -> new NotFoundException("School not found"));
        return TenantContext.callAs(tenant.id(), () -> transactions.execute(status -> {
            BranchInfo branch = branches.findByCode(branchCode)
                    .orElseThrow(() -> new NotFoundException("Branch " + branchCode + " not found"));
            ApplicationForm withBranch = new ApplicationForm(branch.id(), form.academicYearId(), form.gradeLevel(),
                    form.boardingRequested(), form.learner(), form.guardian(), form.notes());
            return open(withBranch, Source.PUBLIC_ENQUIRY, ApplicationStatus.ENQUIRY).getReference();
        }));
    }

    @Transactional
    public Detail create(ApplicationForm form) {
        access.require(Permission.ADMISSIONS_MANAGE, form.branchId());
        AdmissionApplication application = open(form, Source.STAFF, ApplicationStatus.APPLIED);
        return detail(application);
    }

    @Transactional(readOnly = true)
    public List<Summary> list(UUID branchId, ApplicationStatus status) {
        branches.require(branchId);
        access.require(Permission.ADMISSIONS_VIEW, branchId);
        List<AdmissionApplication> found = status == null
                ? applications.findByBranchIdOrderByCreatedAtDesc(branchId)
                : applications.findByBranchIdAndStatusOrderByCreatedAtDesc(branchId, status);
        return found.stream().map(Admissions::summary).toList();
    }

    @Transactional(readOnly = true)
    public Detail get(UUID applicationId) {
        AdmissionApplication application = load(applicationId);
        access.require(Permission.ADMISSIONS_VIEW, application.getBranchId());
        return detail(application);
    }

    @Transactional
    public Detail transition(UUID applicationId, ApplicationStatus target, String note, LocalDate assessmentDate,
            LocalDate offerExpiresOn) {
        AdmissionApplication application = load(applicationId);
        access.require(Permission.ADMISSIONS_MANAGE, application.getBranchId());
        if (target == ApplicationStatus.ADMITTED) {
            throw new BadRequestException("Use the admit action to admit a learner");
        }
        if (target == ApplicationStatus.ASSESSMENT_SCHEDULED && assessmentDate == null) {
            throw new BadRequestException("Give the assessment date");
        }
        ApplicationStatus previous = application.moveTo(target, assessmentDate, offerExpiresOn);
        record(application, previous, target, note);
        return detail(application);
    }

    @Transactional
    public AdmissionResult admit(UUID applicationId, UUID streamId) {
        AdmissionApplication application = load(applicationId);
        access.require(Permission.ADMISSIONS_MANAGE, application.getBranchId());
        if (application.getStatus() != ApplicationStatus.ACCEPTED) {
            throw new ConflictException(
                    "Only accepted offers can be admitted; this application is " + application.getStatus());
        }
        Learners.Admitted admitted = learners.admit(new Learners.AdmitCommand(application.getBranchId(), streamId,
                application.getLearnerGivenName(), application.getLearnerFamilyName(), null,
                application.getLearnerDateOfBirth(), application.getLearnerGender(), application.getGradeLevel(),
                application.isBoardingRequested() ? BoardingStatus.BOARDING : BoardingStatus.DAY,
                new Learners.GuardianDetails(application.getGuardianGivenName(), application.getGuardianFamilyName(),
                        application.getGuardianPhone(), application.getGuardianEmail(),
                        application.getGuardianRelationship())));
        application.markAdmitted(admitted.learnerId());
        record(application, ApplicationStatus.ACCEPTED, ApplicationStatus.ADMITTED,
                "Admission number " + admitted.admissionNumber());
        return new AdmissionResult(admitted.learnerId(), admitted.admissionNumber());
    }

    private AdmissionApplication open(ApplicationForm form, Source source, ApplicationStatus initial) {
        BranchInfo branch = branches.require(form.branchId());
        if (form.boardingRequested() && !branch.boardingEnabled()) {
            throw new BadRequestException(branch.name() + " does not offer boarding");
        }
        UUID academicYearId = form.academicYearId() != null ? form.academicYearId()
                : calendar.currentAcademicYearId().orElse(null);
        String phone = PhoneNumbers.require(form.guardian().phone(), "Guardian phone");
        String email = PhoneNumbers.normalizeEmail(form.guardian().email()).orElse(null);
        int year = LocalDate.now(clock).getYear();
        String reference = String.format(Locale.ROOT, "APP-%d-%04d", year, sequences.next("APP-" + year));
        AdmissionApplication application = applications.save(new AdmissionApplication(branch.id(), academicYearId,
                reference, source, initial, form.learner().givenName(), form.learner().familyName(),
                form.learner().dateOfBirth(), form.learner().gender(), form.gradeLevel(), form.boardingRequested(),
                form.guardian().givenName(), form.guardian().familyName(), phone, email,
                form.guardian().relationship(), form.notes()));
        record(application, null, initial, source == Source.PUBLIC_ENQUIRY ? "Submitted online" : null);
        return application;
    }

    private void record(AdmissionApplication application, ApplicationStatus from, ApplicationStatus to, String note) {
        UUID actorId = ActorContext.current().map(Actor::personId).orElse(null);
        events.save(new AdmissionEvent(application.getId(), from, to, note, actorId, Instant.now(clock)));
    }

    private AdmissionApplication load(UUID applicationId) {
        return applications.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));
    }

    private static Summary summary(AdmissionApplication a) {
        return new Summary(a.getId(), a.getReference(), a.getBranchId(),
                a.getLearnerGivenName() + " " + a.getLearnerFamilyName(), a.getGradeLevel(), a.getStatus(),
                a.getGuardianGivenName() + " " + a.getGuardianFamilyName(), a.getGuardianPhone(), a.getSource(),
                a.getCreatedAt());
    }

    private Detail detail(AdmissionApplication a) {
        List<AdmissionEvent> history = events.findByApplicationIdOrderByOccurredAtAsc(a.getId());
        Map<UUID, PersonRef> actors = people.findAll(history.stream()
                .map(AdmissionEvent::getActorPersonId).filter(Objects::nonNull).distinct().toList());
        List<HistoryEntry> entries = history.stream()
                .map(event -> new HistoryEntry(event.getFromStatus(), event.getToStatus(), event.getNote(),
                        event.getActorPersonId() == null ? "Parent (online)"
                                : actors.containsKey(event.getActorPersonId())
                                        ? actors.get(event.getActorPersonId()).fullName() : null,
                        event.getOccurredAt()))
                .toList();
        String admissionNumber = a.getLearnerId() == null ? null
                : learnerDirectory.find(a.getLearnerId()).map(LearnerSummary::admissionNumber).orElse(null);
        Summary s = summary(a);
        return new Detail(s.id(), s.reference(), s.branchId(), s.learnerName(), s.gradeLevel(), s.status(),
                s.guardianName(), s.guardianPhone(), s.source(), s.createdAt(), a.getAcademicYearId(),
                new LearnerDetails(a.getLearnerGivenName(), a.getLearnerFamilyName(), a.getLearnerDateOfBirth(),
                        a.getLearnerGender()),
                new GuardianDetails(a.getGuardianGivenName(), a.getGuardianFamilyName(), a.getGuardianPhone(),
                        a.getGuardianEmail(), a.getGuardianRelationship()),
                a.isBoardingRequested(), a.getNotes(), a.getAssessmentDate(), a.getOfferExpiresOn(), a.getLearnerId(),
                admissionNumber, List.copyOf(a.getStatus().allowedTransitions()), entries);
    }
}
