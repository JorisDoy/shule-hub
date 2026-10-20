package ke.shulehub.assessment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Turns marks into CBE bands. Percentages are rounded half-up to a whole number first (KNEC's
 * bands are whole percentages), then the band is the one with the highest minimum at or below
 * it. So 74.5% → 75% → EE2 on the KNEC scale.
 */
public final class Banding {

    public record Band(String code, String label, BigDecimal minPercent, BigDecimal maxPercent, int points,
            String descriptor) {
    }

    private Banding() {
    }

    public static int percent(BigDecimal rawScore, BigDecimal maxScore) {
        if (rawScore == null || maxScore == null || maxScore.signum() <= 0) {
            throw new IllegalArgumentException("Need a score and a positive maximum");
        }
        return rawScore.multiply(BigDecimal.valueOf(100)).divide(maxScore, 0, RoundingMode.HALF_UP).intValueExact();
    }

    public static int roundPercent(BigDecimal percent) {
        return percent.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    public static Optional<Band> forPercent(List<Band> bands, int percent) {
        BigDecimal value = BigDecimal.valueOf(percent);
        return bands.stream()
                .sorted(Comparator.comparing(Band::minPercent).reversed())
                .filter(band -> value.compareTo(band.minPercent()) >= 0)
                .findFirst();
    }

    /** The band with exactly these points, else the closest lower one. */
    public static Optional<Band> forPoints(List<Band> bands, int points) {
        return bands.stream()
                .sorted(Comparator.comparingInt(Band::points).reversed())
                .filter(band -> band.points() <= points)
                .findFirst();
    }

    public static Optional<Band> byCode(List<Band> bands, String code) {
        return bands.stream().filter(band -> band.code().equalsIgnoreCase(code)).findFirst();
    }
}
