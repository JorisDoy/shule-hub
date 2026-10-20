package ke.shulehub.assessment.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GradingScaleRepository extends JpaRepository<GradingScale, UUID> {

    @Query("select distinct s from GradingScale s left join fetch s.bands order by s.code")
    List<GradingScale> findAllWithBands();

    boolean existsByCode(String code);
}
