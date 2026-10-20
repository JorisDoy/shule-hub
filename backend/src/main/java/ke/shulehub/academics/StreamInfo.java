package ke.shulehub.academics;

import java.util.UUID;

import ke.shulehub.shared.school.GradeLevel;

public record StreamInfo(UUID id, UUID branchId, UUID academicYearId, GradeLevel gradeLevel, String name, int capacity,
        UUID classTeacherPersonId) {
}
