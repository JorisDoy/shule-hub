package ke.shulehub.learners.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrolmentRepository extends JpaRepository<Enrolment, UUID> {

    List<Enrolment> findByStreamIdAndStatus(UUID streamId, String status);

    List<Enrolment> findByLearnerIdInAndStatus(Collection<UUID> learnerIds, String status);

    Optional<Enrolment> findByLearnerIdAndAcademicYearIdAndStatus(UUID learnerId, UUID academicYearId, String status);

    long countByStreamIdAndStatus(UUID streamId, String status);
}
