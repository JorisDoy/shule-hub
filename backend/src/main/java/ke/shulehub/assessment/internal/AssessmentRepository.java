package ke.shulehub.assessment.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    List<Assessment> findByStreamIdAndTermIdOrderByAssessedOnAscTitleAsc(UUID streamId, UUID termId);

    List<Assessment> findByStreamIdOrderByAssessedOnDesc(UUID streamId);
}
