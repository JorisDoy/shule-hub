package ke.shulehub.assessment.internal;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentScoreRepository extends JpaRepository<AssessmentScore, UUID> {

    List<AssessmentScore> findByAssessmentId(UUID assessmentId);

    List<AssessmentScore> findByAssessmentIdIn(Collection<UUID> assessmentIds);

    List<AssessmentScore> findByAssessmentIdInAndLearnerId(Collection<UUID> assessmentIds, UUID learnerId);
}
