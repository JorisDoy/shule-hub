package ke.shulehub.academics.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamRepository extends JpaRepository<Stream, UUID> {

    List<Stream> findByBranchIdAndAcademicYearIdOrderByGradeLevelAscNameAsc(UUID branchId, UUID academicYearId);

    List<Stream> findByBranchIdOrderByGradeLevelAscNameAsc(UUID branchId);
}
