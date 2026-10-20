package ke.shulehub.academics.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import ke.shulehub.shared.persistence.TenantScopedEntity;

@Entity
@Table(name = "academic_year")
public class AcademicYear extends TenantScopedEntity {

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number ASC")
    private List<Term> terms = new ArrayList<>();

    protected AcademicYear() {
    }

    public AcademicYear(int year, LocalDate startDate, LocalDate endDate) {
        this.year = year;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Term addTerm(int number, LocalDate start, LocalDate end) {
        Term term = new Term(this, number, start, end);
        terms.add(term);
        return term;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public int getYear() {
        return year;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<Term> getTerms() {
        return terms;
    }
}
