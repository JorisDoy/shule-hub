package ke.shulehub.learners;

import java.util.UUID;

import ke.shulehub.shared.school.GradeLevel;

/** A learner as lists and other modules see them. Stream and phone may be null. */
public record LearnerSummary(UUID id, UUID branchId, String admissionNumber, String givenName, String familyName,
        String fullName, GradeLevel gradeLevel, BoardingStatus boardingStatus, LearnerStatus status, UUID streamId,
        String streamName, String primaryContactPhone) {
}
