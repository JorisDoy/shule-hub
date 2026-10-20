package ke.shulehub.academics.internal;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ke.shulehub.academics.StreamInfo;
import ke.shulehub.shared.persistence.TenantScopedEntity;
import ke.shulehub.shared.school.GradeLevel;

/** A class for one academic year, such as "Grade 4 East" at the Ruaka branch. */
@Entity
@Table(name = "stream")
public class Stream extends TenantScopedEntity {

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    @Column(name = "academic_year_id", nullable = false, updatable = false)
    private UUID academicYearId;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_level", nullable = false)
    private GradeLevel gradeLevel;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "class_teacher_person_id")
    private UUID classTeacherPersonId;

    protected Stream() {
    }

    public Stream(UUID branchId, UUID academicYearId, GradeLevel gradeLevel, String name, int capacity) {
        this.branchId = branchId;
        this.academicYearId = academicYearId;
        this.gradeLevel = gradeLevel;
        this.name = name.trim();
        this.capacity = capacity;
    }

    public StreamInfo toInfo() {
        return new StreamInfo(getId(), branchId, academicYearId, gradeLevel, name, capacity, classTeacherPersonId);
    }

    public void assignClassTeacher(UUID personId) {
        this.classTeacherPersonId = personId;
    }
}
