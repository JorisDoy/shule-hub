package ke.shulehub.fees.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ke.shulehub.fees.FeeStructureStatus;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, UUID> {

    @Query("select distinct s from FeeStructure s left join fetch s.items where s.branchId = :branchId and s.termId = :termId")
    List<FeeStructure> findWithItems(UUID branchId, UUID termId);

    @Query("select distinct s from FeeStructure s left join fetch s.items where s.branchId = :branchId")
    List<FeeStructure> findWithItemsByBranch(UUID branchId);

    @Query("select distinct s from FeeStructure s left join fetch s.items "
            + "where s.branchId = :branchId and s.termId = :termId and s.status = :status")
    List<FeeStructure> findWithItemsByStatus(UUID branchId, UUID termId, FeeStructureStatus status);
}
