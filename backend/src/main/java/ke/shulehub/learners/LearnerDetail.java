package ke.shulehub.learners;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import ke.shulehub.shared.school.GradeLevel;

public record LearnerDetail(UUID id, UUID branchId, String admissionNumber, String givenName, String familyName,
        String fullName, GradeLevel gradeLevel, BoardingStatus boardingStatus, LearnerStatus status, UUID streamId,
        String streamName, String primaryContactPhone, String otherNames, LocalDate dateOfBirth, Gender gender,
        String upi, String assessmentNumber, LocalDate admittedOn, List<Guardian> guardians) {

    public record Guardian(UUID personId, String fullName, String phone, String email, Relationship relationship,
            boolean feePayer, boolean canPickUp, boolean primaryContact) {
    }
}
