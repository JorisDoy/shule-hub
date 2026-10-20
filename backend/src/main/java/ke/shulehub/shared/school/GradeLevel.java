package ke.shulehub.shared.school;

import java.util.Optional;

/** Daycare through Grade 9, in order. */
public enum GradeLevel {
    DAYCARE(LevelBand.DAYCARE, "Daycare"),
    PP1(LevelBand.PRE_PRIMARY, "PP1"),
    PP2(LevelBand.PRE_PRIMARY, "PP2"),
    GRADE_1(LevelBand.LOWER_PRIMARY, "Grade 1"),
    GRADE_2(LevelBand.LOWER_PRIMARY, "Grade 2"),
    GRADE_3(LevelBand.LOWER_PRIMARY, "Grade 3"),
    GRADE_4(LevelBand.UPPER_PRIMARY, "Grade 4"),
    GRADE_5(LevelBand.UPPER_PRIMARY, "Grade 5"),
    GRADE_6(LevelBand.UPPER_PRIMARY, "Grade 6"),
    GRADE_7(LevelBand.JUNIOR_SCHOOL, "Grade 7"),
    GRADE_8(LevelBand.JUNIOR_SCHOOL, "Grade 8"),
    GRADE_9(LevelBand.JUNIOR_SCHOOL, "Grade 9");

    private final LevelBand band;
    private final String label;

    GradeLevel(LevelBand band, String label) {
        this.band = band;
        this.label = label;
    }

    public LevelBand band() {
        return band;
    }

    public String label() {
        return label;
    }

    /** The grade a learner is promoted to, or empty after Grade 9 (they leave for Senior School). */
    public Optional<GradeLevel> next() {
        GradeLevel[] all = values();
        return ordinal() + 1 < all.length ? Optional.of(all[ordinal() + 1]) : Optional.empty();
    }
}
