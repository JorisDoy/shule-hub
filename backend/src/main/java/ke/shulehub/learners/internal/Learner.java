package ke.shulehub.learners.internal;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ke.shulehub.learners.BoardingStatus;
import ke.shulehub.learners.Gender;
import ke.shulehub.learners.LearnerStatus;
import ke.shulehub.shared.persistence.TenantScopedEntity;
import ke.shulehub.shared.school.GradeLevel;

@Entity
@Table(name = "learner")
public class Learner extends TenantScopedEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "admission_number", nullable = false, updatable = false)
    private String admissionNumber;

    @Column(name = "given_name", nullable = false)
    private String givenName;

    @Column(name = "family_name", nullable = false)
    private String familyName;

    @Column(name = "other_names")
    private String otherNames;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_level", nullable = false)
    private GradeLevel gradeLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "boarding_status", nullable = false)
    private BoardingStatus boardingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LearnerStatus status = LearnerStatus.ACTIVE;

    /** Unique Personal Identifier used by KEMIS. */
    @Column(name = "upi")
    private String upi;

    /** KNEC assessment number (KPSEA / KJSEA). */
    @Column(name = "assessment_number")
    private String assessmentNumber;

    @Column(name = "admitted_on", nullable = false)
    private LocalDate admittedOn;

    protected Learner() {
    }

    public Learner(UUID branchId, String admissionNumber, String givenName, String familyName, String otherNames,
            LocalDate dateOfBirth, Gender gender, GradeLevel gradeLevel, BoardingStatus boardingStatus,
            LocalDate admittedOn) {
        this.branchId = branchId;
        this.admissionNumber = normalizeAdmissionNumber(admissionNumber);
        this.givenName = givenName.trim();
        this.familyName = familyName.trim();
        this.otherNames = otherNames == null || otherNames.isBlank() ? null : otherNames.trim();
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.gradeLevel = gradeLevel;
        this.boardingStatus = boardingStatus;
        this.admittedOn = admittedOn;
    }

    /** Upper-case letters and digits only, so "rua-0042" and "RUA 0042" both match RUA0042. */
    public static String normalizeAdmissionNumber(String raw) {
        return raw == null ? "" : raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    public void updateIdentifiers(String newUpi, String newAssessmentNumber) {
        if (newUpi != null) {
            this.upi = newUpi.isBlank() ? null : newUpi.trim().toUpperCase(Locale.ROOT);
        }
        if (newAssessmentNumber != null) {
            this.assessmentNumber = newAssessmentNumber.isBlank() ? null : newAssessmentNumber.trim();
        }
    }

    public void changeBoardingStatus(BoardingStatus newStatus) {
        this.boardingStatus = newStatus;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public String getAdmissionNumber() {
        return admissionNumber;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getOtherNames() {
        return otherNames;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public GradeLevel getGradeLevel() {
        return gradeLevel;
    }

    public BoardingStatus getBoardingStatus() {
        return boardingStatus;
    }

    public LearnerStatus getStatus() {
        return status;
    }

    public String getUpi() {
        return upi;
    }

    public String getAssessmentNumber() {
        return assessmentNumber;
    }

    public LocalDate getAdmittedOn() {
        return admittedOn;
    }
}
