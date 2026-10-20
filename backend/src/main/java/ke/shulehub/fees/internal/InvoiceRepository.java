package ke.shulehub.fees.internal;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ke.shulehub.fees.InvoiceStatus;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    boolean existsByLearnerIdAndTermIdAndFeeStructureId(UUID learnerId, UUID termId, UUID feeStructureId);

    @Query("select distinct i from Invoice i join fetch i.lines where i.learnerId = :learnerId and i.status in :statuses")
    List<Invoice> findWithLinesForLearner(UUID learnerId, Collection<InvoiceStatus> statuses);

    @Query("select distinct i from Invoice i left join fetch i.lines where i.id = :id")
    List<Invoice> findWithLines(UUID id);

    List<Invoice> findByLearnerIdOrderByIssuedOnAsc(UUID learnerId);

    List<Invoice> findByLearnerIdInAndStatusIn(Collection<UUID> learnerIds, Collection<InvoiceStatus> statuses);

    List<Invoice> findByBranchIdAndStatusIn(UUID branchId, Collection<InvoiceStatus> statuses);

    List<Invoice> findByBranchIdAndTermIdOrderByNumberAsc(UUID branchId, UUID termId);

    List<Invoice> findByBranchIdOrderByIssuedOnDescNumberDesc(UUID branchId);
}
