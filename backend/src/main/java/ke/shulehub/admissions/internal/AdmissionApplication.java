package ke.shulehub.admissions.internal;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ke.shulehub.admissions.ApplicationStatus;
import ke.shulehub.learners.Gender;
import ke.shulehub.learners.Relationship;
import ke.shulehub.shared.persistence.TenantScopedEntity;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.web.ConflictException;

@Entity
@Table(name = "admission_application")
public class AdmissionApplication extends TenantScopedEntity {

    public enum Source {
        PUBLIC_ENQUIRY,
        STAFF
    }

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "academic_year_id")
    private UUID academicYearId;

    @Column(name = "reference", nullable = false, updatable = false)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, updatable = false)
    private Source source;

    @Column(name = "learner_given_name", nullable = false)
    private String learnerGivenName;

    @Column(name = "learner_family_name", nullable = false)
    private String learnerFamilyName;

    @Column(name = "learner_date_of_birth")
    private LocalDate learnerDateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "learner_gender")
    private Gender learnerGender;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_level", nullable = false)
    private GradeLevel gradeLevel;

    @Column(name = "boarding_requested", nullable = false)
    private boolean boardingRequested;

    @Column(name = "guardian_given_name", nullable = false)
    private String guardianGivenName;

    @Column(name = "guardian_family_name", nullable = false)
    private String guardianFamilyName;

    @Column(name = "guardian_phone", nullable = false)
    private String guardianPhone;

    @Column(name = "guardian_email")
    private String guardianEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "guardian_relationship", nullable = false)
    private Relationship guardianRelationship;

    @Column(name = "notes")
    private String notes;

    @Column(name = "assessment_date")
    private LocalDate assessmentDate;

    @Column(name = "offer_expires_on")
    private LocalDate offerExpiresOn;

    @Column(name = "learner_id")
    private UUID learnerId;

    protected AdmissionApplication() {
    }

    public AdmissionApplication(UUID branchId, UUID academicYearId, String reference, Source source,
            ApplicationStatus status, String learnerGivenName, String learnerFamilyName, LocalDate learnerDateOfBirth,
            Gender learnerGender, GradeLevel gradeLevel, boolean boardingRequested, String guardianGivenName,
            String guardianFamilyName, String guardianPhone, String guardianEmail, Relationship guardianRelationship,
            String notes) {
        this.branchId = branchId;
        this.academicYearId = academicYearId;
        this.reference = reference;
        this.source = source;
        this.status = status;
        this.learnerGivenName = learnerGivenName.trim();
        this.learnerFamilyName = learnerFamilyName.trim();
        this.learnerDateOfBirth = learnerDateOfBirth;
        this.learnerGender = learnerGender;
        this.gradeLevel = gradeLevel;
        this.boardingRequested = boardingRequested;
        this.guardianGivenName = guardianGivenName.trim();
        this.guardianFamilyName = guardianFamilyName.trim();
        this.guardianPhone = guardianPhone;
        this.guardianEmail = guardianEmail;
        this.guardianRelationship = guardianRelationship;
        this.notes = notes;
    }

    /** Moves to {@code target} if the workflow allows it; returns the previous status. */
    public ApplicationStatus moveTo(ApplicationStatus target, LocalDate newAssessmentDate, LocalDate newOfferExpiresOn) {
        if (!status.canMoveTo(target)) {
            throw new ConflictException("An application that is " + status + " cannot move to " + target);
        }
        ApplicationStatus previous = status;
        status = target;
        if (target == ApplicationStatus.ASSESSMENT_SCHEDULED) {
            assessmentDate = newAssessmentDate;
        }
        if (target == ApplicationStatus.OFFERED) {
            offerExpiresOn = newOfferExpiresOn;
        }
        return previous;
    }

    public void markAdmitted(UUID admittedLearnerId) {
        if (status != ApplicationStatus.ACCEPTED) {
            throw new ConflictException("Only accepted offers can be admitted; this application is " + status);
        }
        status = ApplicationStatus.ADMITTED;
        learnerId = admittedLearnerId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getAcademicYearId() {
        return academicYearId;
    }

    public String getReference() {
        return reference;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public Source getSource() {
        return source;
    }

    public String getLearnerGivenName() {
        return learnerGivenName;
    }

    public String getLearnerFamilyName() {
        return learnerFamilyName;
    }

    public LocalDate getLearnerDateOfBirth() {
        return learnerDateOfBirth;
    }

    public Gender getLearnerGender() {
        return learnerGender;
    }

    public GradeLevel getGradeLevel() {
        return gradeLevel;
    }

    public boolean isBoardingRequested() {
        return boardingRequested;
    }

    public String getGuardianGivenName() {
        return guardianGivenName;
    }

    public String getGuardianFamilyName() {
        return guardianFamilyName;
    }

    public String getGuardianPhone() {
        return guardianPhone;
    }

    public String getGuardianEmail() {
        return guardianEmail;
    }

    public Relationship getGuardianRelationship() {
        return guardianRelationship;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    public LocalDate getOfferExpiresOn() {
        return offerExpiresOn;
    }

    public UUID getLearnerId() {
        return learnerId;
    }
}
