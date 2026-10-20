package ke.shulehub.academics.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ke.shulehub.academics.LearningAreaInfo;
import ke.shulehub.shared.persistence.TenantScopedEntity;
import ke.shulehub.shared.school.LevelBand;

@Entity
@Table(name = "learning_area")
public class LearningArea extends TenantScopedEntity {

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_band", nullable = false)
    private LevelBand levelBand;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected LearningArea() {
    }

    public LearningArea(String code, String name, LevelBand levelBand, int sortOrder) {
        this.code = code;
        this.name = name;
        this.levelBand = levelBand;
        this.sortOrder = sortOrder;
    }

    public LearningAreaInfo toInfo() {
        return new LearningAreaInfo(getId(), code, name, levelBand, sortOrder);
    }
}
