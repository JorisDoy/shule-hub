package ke.shulehub.academics.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ke.shulehub.shared.school.LevelBand;

public interface LearningAreaRepository extends JpaRepository<LearningArea, UUID> {

    List<LearningArea> findByLevelBandAndActiveTrueOrderBySortOrderAscNameAsc(LevelBand levelBand);

    List<LearningArea> findByActiveTrueOrderByLevelBandAscSortOrderAsc();

    boolean existsByLevelBand(LevelBand levelBand);
}
