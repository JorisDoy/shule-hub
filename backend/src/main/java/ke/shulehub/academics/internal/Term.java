package ke.shulehub.academics.internal;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import ke.shulehub.academics.TermInfo;
import ke.shulehub.shared.persistence.TenantScopedEntity;

@Entity
@Table(name = "term")
public class Term extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false, updatable = false)
    private AcademicYear academicYear;

    @Column(name = "number", nullable = false)
    private int number;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    protected Term() {
    }

    Term(AcademicYear academicYear, int number, LocalDate startDate, LocalDate endDate) {
        this.academicYear = academicYear;
        this.number = number;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public TermInfo toInfo() {
        return new TermInfo(getId(), academicYear.getId(), academicYear.getYear(), number, startDate, endDate);
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public int getNumber() {
        return number;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
