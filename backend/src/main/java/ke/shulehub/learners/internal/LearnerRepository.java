package ke.shulehub.learners.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ke.shulehub.learners.LearnerStatus;

public interface LearnerRepository extends JpaRepository<Learner, UUID>, JpaSpecificationExecutor<Learner> {

    Optional<Learner> findByAdmissionNumber(String admissionNumber);

    List<Learner> findByBranchIdAndStatus(UUID branchId, LearnerStatus status);
}
