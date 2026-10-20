package ke.shulehub.academics;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AcademicYearInfo(UUID id, int year, LocalDate startDate, LocalDate endDate, boolean current,
        List<TermSummary> terms) {

    public record TermSummary(UUID id, int number, LocalDate startDate, LocalDate endDate) {
    }
}
