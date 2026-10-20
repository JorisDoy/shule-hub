package ke.shulehub.academics;

import java.time.LocalDate;
import java.util.UUID;

public record TermInfo(UUID id, UUID academicYearId, int year, int number, LocalDate startDate, LocalDate endDate) {

    public String label() {
        return "Term " + number + " " + year;
    }
}
