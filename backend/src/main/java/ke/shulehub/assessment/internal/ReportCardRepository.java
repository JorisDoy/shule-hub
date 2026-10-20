package ke.shulehub.assessment.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ke.shulehub.assessment.ReportCardStatus;

public interface ReportCardRepository extends JpaRepository<ReportCard, UUID> {

    Optional<ReportCard> findByLearnerIdAndTermId(UUID learnerId, UUID termId);

    List<ReportCard> findByStreamIdAndTermId(UUID streamId, UUID termId);

    List<ReportCard> findByLearnerIdAndStatus(UUID learnerId, ReportCardStatus status);
}
