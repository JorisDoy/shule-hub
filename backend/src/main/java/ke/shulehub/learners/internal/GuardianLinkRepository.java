package ke.shulehub.learners.internal;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianLinkRepository extends JpaRepository<GuardianLink, UUID> {

    List<GuardianLink> findByLearnerId(UUID learnerId);

    List<GuardianLink> findByLearnerIdIn(Collection<UUID> learnerIds);

    List<GuardianLink> findByPersonId(UUID personId);

    boolean existsByPersonIdAndLearnerId(UUID personId, UUID learnerId);
}
