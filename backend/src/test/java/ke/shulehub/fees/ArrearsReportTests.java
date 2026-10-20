package ke.shulehub.fees;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ArrearsReportTests {

    private final LocalDate asOf = LocalDate.parse("2027-04-30");

    @Test
    void bucketsByDaysPastTheDueDate() {
        assertThat(ArrearsReport.bucket(LocalDate.parse("2027-05-15"), asOf)).isZero();
        assertThat(ArrearsReport.bucket(asOf, asOf)).isZero();
        assertThat(ArrearsReport.bucket(asOf.minusDays(1), asOf)).isEqualTo(1);
        assertThat(ArrearsReport.bucket(asOf.minusDays(30), asOf)).isEqualTo(1);
        assertThat(ArrearsReport.bucket(asOf.minusDays(31), asOf)).isEqualTo(2);
        assertThat(ArrearsReport.bucket(asOf.minusDays(61), asOf)).isEqualTo(3);
        assertThat(ArrearsReport.bucket(asOf.minusDays(91), asOf)).isEqualTo(4);
    }
}
