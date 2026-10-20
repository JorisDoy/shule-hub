package ke.shulehub.academics;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.academics.internal.LearningArea;
import ke.shulehub.academics.internal.LearningAreaRepository;
import ke.shulehub.shared.school.LevelBand;
import ke.shulehub.shared.web.NotFoundException;
import ke.shulehub.tenancy.TenantProvisioned;

/** Learning areas per CBE band, seeded for each new tenant and editable afterwards. */
@Service
public class LearningAreas {

    private record Seed(String code, String name) {
    }

    /**
     * Defaults from the rationalised CBE curriculum. Schools should check them against the
     * current KICD curriculum designs; daycare areas are developmental domains, not CBE.
     */
    private static final Map<LevelBand, List<Seed>> CATALOGUE = Map.of(
            LevelBand.DAYCARE, List.of(
                    new Seed("COMM", "Communication and Language"),
                    new Seed("PHYS", "Physical Development"),
                    new Seed("SOCIAL", "Social and Emotional Development"),
                    new Seed("EXPLORE", "Exploration and Play")),
            LevelBand.PRE_PRIMARY, List.of(
                    new Seed("LANG", "Language Activities"),
                    new Seed("MATH", "Mathematical Activities"),
                    new Seed("CREATIVE", "Creative Activities"),
                    new Seed("ENV", "Environmental Activities"),
                    new Seed("RE", "Religious Education Activities")),
            LevelBand.LOWER_PRIMARY, List.of(
                    new Seed("IND", "Indigenous Language Activities"),
                    new Seed("KIS", "Kiswahili Language Activities"),
                    new Seed("ENG", "English Language Activities"),
                    new Seed("MATH", "Mathematical Activities"),
                    new Seed("RE", "Religious Education Activities"),
                    new Seed("ENV", "Environmental Activities"),
                    new Seed("CREATIVE", "Creative Activities")),
            LevelBand.UPPER_PRIMARY, List.of(
                    new Seed("ENG", "English"),
                    new Seed("KIS", "Kiswahili"),
                    new Seed("MATH", "Mathematics"),
                    new Seed("RE", "Religious Education"),
                    new Seed("SCITECH", "Science and Technology"),
                    new Seed("AGRINUT", "Agriculture and Nutrition"),
                    new Seed("SST", "Social Studies"),
                    new Seed("CA", "Creative Arts")),
            LevelBand.JUNIOR_SCHOOL, List.of(
                    new Seed("ENG", "English"),
                    new Seed("KIS", "Kiswahili"),
                    new Seed("MATH", "Mathematics"),
                    new Seed("RE", "Religious Education"),
                    new Seed("ISCI", "Integrated Science"),
                    new Seed("PTS", "Pre-Technical Studies"),
                    new Seed("AGRINUT", "Agriculture and Nutrition"),
                    new Seed("SST", "Social Studies"),
                    new Seed("CAS", "Creative Arts and Sports")));

    private final LearningAreaRepository areas;

    public LearningAreas(LearningAreaRepository areas) {
        this.areas = areas;
    }

    @EventListener
    @Transactional
    public void seedDefaults(TenantProvisioned event) {
        CATALOGUE.forEach((band, seeds) -> {
            if (!areas.existsByLevelBand(band)) {
                for (int i = 0; i < seeds.size(); i++) {
                    areas.save(new LearningArea(seeds.get(i).code(), seeds.get(i).name(), band, (i + 1) * 10));
                }
            }
        });
    }

    @Transactional(readOnly = true)
    public List<LearningAreaInfo> list(LevelBand band) {
        List<LearningArea> found = band == null
                ? areas.findByActiveTrueOrderByLevelBandAscSortOrderAsc()
                : areas.findByLevelBandAndActiveTrueOrderBySortOrderAscNameAsc(band);
        return found.stream().map(LearningArea::toInfo).toList();
    }

    @Transactional(readOnly = true)
    public LearningAreaInfo require(UUID learningAreaId) {
        return areas.findById(learningAreaId).map(LearningArea::toInfo)
                .orElseThrow(() -> new NotFoundException("Learning area not found"));
    }

    @Transactional(readOnly = true)
    public Map<UUID, LearningAreaInfo> findAll(Collection<UUID> ids) {
        return areas.findAllById(ids).stream()
                .map(LearningArea::toInfo)
                .collect(Collectors.toMap(LearningAreaInfo::id, Function.identity()));
    }
}
