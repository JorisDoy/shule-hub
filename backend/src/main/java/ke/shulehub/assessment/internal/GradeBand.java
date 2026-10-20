package ke.shulehub.assessment.internal;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import ke.shulehub.assessment.Banding;
import ke.shulehub.shared.persistence.TenantScopedEntity;

@Entity
@Table(name = "grade_band")
public class GradeBand extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grading_scale_id", nullable = false, updatable = false)
    private GradingScale scale;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "min_percent", nullable = false)
    private BigDecimal minPercent;

    @Column(name = "max_percent", nullable = false)
    private BigDecimal maxPercent;

    @Column(name = "points", nullable = false)
    private int points;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "descriptor")
    private String descriptor;

    protected GradeBand() {
    }

    GradeBand(GradingScale scale, String code, String label, BigDecimal minPercent, BigDecimal maxPercent, int points,
            int sortOrder, String descriptor) {
        this.scale = scale;
        this.code = code;
        this.label = label;
        this.minPercent = minPercent;
        this.maxPercent = maxPercent;
        this.points = points;
        this.sortOrder = sortOrder;
        this.descriptor = descriptor;
    }

    Banding.Band toBand() {
        return new Banding.Band(code, label, minPercent, maxPercent, points, descriptor);
    }

    public BigDecimal getMinPercent() {
        return minPercent;
    }
}
