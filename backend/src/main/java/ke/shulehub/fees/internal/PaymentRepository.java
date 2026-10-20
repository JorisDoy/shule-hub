package ke.shulehub.fees.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ke.shulehub.fees.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByDedupeKey(String dedupeKey);

    List<Payment> findByLearnerIdOrderByReceivedAtAsc(UUID learnerId);

    List<Payment> findByLearnerIdAndStatusOrderByReceivedAtAsc(UUID learnerId, PaymentStatus status);

    List<Payment> findByBranchIdOrderByReceivedAtDesc(UUID branchId);

    List<Payment> findByBranchIdAndStatusOrderByReceivedAtDesc(UUID branchId, PaymentStatus status);
}
