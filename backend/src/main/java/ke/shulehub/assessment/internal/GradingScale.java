package ke.shulehub.assessment.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import ke.shulehub.assessment.Banding;
import ke.shulehub.shared.persistence.TenantScopedEntity;
import ke.shulehub.shared.school.LevelBand;

/** A banding scheme, such as the CBE four-level rubric or KNEC's eight-level scale. */
@Entity
@Table(name = "grading_scale")
public class GradingScale extends TenantScopedEntity {

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    /** Comma-separated {@link LevelBand} names. */
    @Column(name = "applies_to", nullable = false)
    private String appliesTo;

    @OneToMany(mappedBy = "scale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GradeBand> bands = new ArrayList<>();

    protected GradingScale() {
    }

    public GradingScale(String code, String name, List<LevelBand> levels) {
        this.code = code;
        this.name = name;
        this.appliesTo = levels.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    public void addBand(String bandCode, String label, int minPercent, int maxPercent, int points, String descriptor) {
        bands.add(new GradeBand(this, bandCode, label, BigDecimal.valueOf(minPercent), BigDecimal.valueOf(maxPercent),
                points, bands.size() + 1, descriptor));
    }

    public List<LevelBand> levels() {
        return Arrays.stream(appliesTo.split(",")).filter(s -> !s.isBlank()).map(LevelBand::valueOf).toList();
    }

    public boolean appliesTo(LevelBand band) {
        return levels().contains(band);
    }

    /** Bands from highest to lowest. */
    public List<Banding.Band> bandViews() {
        return bands.stream()
                .sorted(Comparator.comparing(GradeBand::getMinPercent).reversed())
                .map(GradeBand::toBand)
                .toList();
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
