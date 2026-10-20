package ke.shulehub.payments.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MpesaChannelRepository extends JpaRepository<MpesaChannel, UUID> {

    /** Call under the root tenant context: callbacks arrive before the tenant is known. */
    Optional<MpesaChannel> findByCallbackToken(String callbackToken);

    Optional<MpesaChannel> findFirstByBranchIdAndActiveTrueOrderByCreatedAtDesc(UUID branchId);

    List<MpesaChannel> findAllByOrderByCreatedAtDesc();
}
