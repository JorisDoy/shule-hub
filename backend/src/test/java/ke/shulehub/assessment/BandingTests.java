package ke.shulehub.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import ke.shulehub.assessment.Banding.Band;

class BandingTests {

    private static final List<Band> KNEC = List.of(
            band("EE1", 90, 8), band("EE2", 75, 7), band("ME1", 58, 6), band("ME2", 41, 5),
            band("AE1", 31, 4), band("AE2", 21, 3), band("BE1", 11, 2), band("BE2", 0, 1));

    @Test
    void roundsHalfUpToAWholePercentBeforeBanding() {
        int percent = Banding.percent(new BigDecimal("37.25"), new BigDecimal("50"));
        assertThat(percent).isEqualTo(75);
        assertThat(Banding.forPercent(KNEC, percent)).map(Band::code).hasValue("EE2");
    }

    @Test
    void mapsBoundariesToTheRightBand() {
        assertThat(Banding.forPercent(KNEC, 100)).map(Band::code).hasValue("EE1");
        assertThat(Banding.forPercent(KNEC, 90)).map(Band::code).hasValue("EE1");
        assertThat(Banding.forPercent(KNEC, 89)).map(Band::code).hasValue("EE2");
        assertThat(Banding.forPercent(KNEC, 74)).map(Band::code).hasValue("ME1");
        assertThat(Banding.forPercent(KNEC, 41)).map(Band::code).hasValue("ME2");
        assertThat(Banding.forPercent(KNEC, 40)).map(Band::code).hasValue("AE1");
        assertThat(Banding.forPercent(KNEC, 11)).map(Band::code).hasValue("BE1");
        assertThat(Banding.forPercent(KNEC, 10)).map(Band::code).hasValue("BE2");
        assertThat(Banding.forPercent(KNEC, 0)).map(Band::code).hasValue("BE2");
    }

    @Test
    void findsBandsByPointsAndCode() {
        assertThat(Banding.forPoints(KNEC, 6)).map(Band::code).hasValue("ME1");
        assertThat(Banding.byCode(KNEC, "ee2")).map(Band::points).hasValue(7);
    }

    private static Band band(String code, int min, int points) {
        return new Band(code, code, BigDecimal.valueOf(min), BigDecimal.valueOf(100), points, null);
    }
}
