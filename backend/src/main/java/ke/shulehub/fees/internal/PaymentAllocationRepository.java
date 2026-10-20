package ke.shulehub.fees.internal;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, UUID> {
}
