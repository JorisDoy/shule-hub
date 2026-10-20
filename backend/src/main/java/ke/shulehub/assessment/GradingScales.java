package ke.shulehub.assessment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.assessment.internal.GradingScale;
import ke.shulehub.assessment.internal.GradingScaleRepository;
import ke.shulehub.shared.school.LevelBand;
import ke.shulehub.tenancy.TenantProvisioned;

/** Grading scales per CBE band. Seeded for each tenant; stored as data so a KNEC change is an edit. */
@Service
public class GradingScales {

    public record ScaleView(UUID id, String code, String name, List<LevelBand> appliesTo, List<Banding.Band> bands) {
    }

    public static final String CBE_FOUR_LEVEL = "CBE_4_LEVEL";
    public static final String KNEC_EIGHT_LEVEL = "KNEC_8_LEVEL";

    private final GradingScaleRepository scales;

    public GradingScales(GradingScaleRepository scales) {
        this.scales = scales;
    }

    /**
     * KNEC's eight-level scale for Junior School (KJSEA, from December 2025) and a four-level
     * rubric for PP1–Grade 6 whose cut-offs group the eight levels in pairs. Both are editable
     * defaults; schools should confirm them against current KNEC guidance.
     */
    @EventListener
    @Transactional
    public void seedDefaults(TenantProvisioned event) {
        if (!scales.existsByCode(CBE_FOUR_LEVEL)) {
            GradingScale four = new GradingScale(CBE_FOUR_LEVEL, "CBE 4-level rubric",
                    List.of(LevelBand.PRE_PRIMARY, LevelBand.LOWER_PRIMARY, LevelBand.UPPER_PRIMARY));
            four.addBand("EE", "Exceeding Expectations", 75, 100, 4, "Consistently goes beyond the expected competence");
            four.addBand("ME", "Meeting Expectations", 41, 74, 3, "Shows the expected competence");
            four.addBand("AE", "Approaching Expectations", 21, 40, 2, "Developing the expected competence with support");
            four.addBand("BE", "Below Expectations", 0, 20, 1, "Needs significant support");
            scales.save(four);
        }
        if (!scales.existsByCode(KNEC_EIGHT_LEVEL)) {
            GradingScale eight = new GradingScale(KNEC_EIGHT_LEVEL, "KNEC 8-level scale",
                    List.of(LevelBand.JUNIOR_SCHOOL));
            eight.addBand("EE1", "Exceeding Expectations 1", 90, 100, 8, "Exceptional");
            eight.addBand("EE2", "Exceeding Expectations 2", 75, 89, 7, "Very good");
            eight.addBand("ME1", "Meeting Expectations 1", 58, 74, 6, "Good");
            eight.addBand("ME2", "Meeting Expectations 2", 41, 57, 5, "Fair");
            eight.addBand("AE1", "Approaching Expectations 1", 31, 40, 4, "Needs improvement");
            eight.addBand("AE2", "Approaching Expectations 2", 21, 30, 3, "Below average");
            eight.addBand("BE1", "Below Expectations 1", 11, 20, 2, "Well below average");
            eight.addBand("BE2", "Below Expectations 2", 0, 10, 1, "Minimal");
            scales.save(eight);
        }
    }

    @Transactional(readOnly = true)
    public List<ScaleView> all() {
        return scales.findAllWithBands().stream().map(GradingScales::view).toList();
    }

    /** The scale for a band, or empty (daycare is reported with comments only). */
    @Transactional(readOnly = true)
    public Optional<ScaleView> forBand(LevelBand band) {
        return scales.findAllWithBands().stream()
                .filter(scale -> scale.appliesTo(band))
                .findFirst()
                .map(GradingScales::view);
    }

    private static ScaleView view(GradingScale scale) {
        return new ScaleView(scale.getId(), scale.getCode(), scale.getName(), scale.levels(), scale.bandViews());
    }
}
