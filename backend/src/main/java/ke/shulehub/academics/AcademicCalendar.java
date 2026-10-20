package ke.shulehub.academics;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.academics.internal.AcademicYear;
import ke.shulehub.academics.internal.AcademicYearRepository;
import ke.shulehub.academics.internal.Term;
import ke.shulehub.academics.internal.TermRepository;
import ke.shulehub.identity.AccessPolicy;
import ke.shulehub.identity.Permission;
import ke.shulehub.shared.web.BadRequestException;
import ke.shulehub.shared.web.ConflictException;
import ke.shulehub.shared.web.NotFoundException;

/** Academic years and their three terms. Dates differ every year, so they are data. */
@Service
public class AcademicCalendar {

    public record NewTerm(int number, LocalDate startDate, LocalDate endDate) {
    }

    private final AcademicYearRepository years;
    private final TermRepository terms;
    private final AccessPolicy access;
    private final Clock clock;

    public AcademicCalendar(AcademicYearRepository years, TermRepository terms, AccessPolicy access, Clock clock) {
        this.years = years;
        this.terms = terms;
        this.access = access;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AcademicYearInfo> years() {
        LocalDate today = LocalDate.now(clock);
        return years.findAllWithTerms().stream()
                .sorted(Comparator.comparingInt(AcademicYear::getYear).reversed())
                .map(year -> toInfo(year, today))
                .toList();
    }

    @Transactional
    public AcademicYearInfo createYear(int year, LocalDate startDate, LocalDate endDate, List<NewTerm> newTerms) {
        access.requireTenantWide(Permission.ACADEMICS_MANAGE);
        if (!endDate.isAfter(startDate)) {
            throw new BadRequestException("The academic year must end after it starts");
        }
        if (years.existsByYear(year)) {
            throw new ConflictException("Academic year " + year + " already exists");
        }
        validateTerms(startDate, endDate, newTerms);
        AcademicYear academicYear = new AcademicYear(year, startDate, endDate);
        newTerms.stream()
                .sorted(Comparator.comparingInt(NewTerm::number))
                .forEach(term -> academicYear.addTerm(term.number(), term.startDate(), term.endDate()));
        return toInfo(years.save(academicYear), LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public Optional<TermInfo> findTerm(UUID termId) {
        return terms.findById(termId).map(Term::toInfo);
    }

    @Transactional(readOnly = true)
    public TermInfo requireTerm(UUID termId) {
        if (termId == null) {
            throw new NotFoundException("Choose a term");
        }
        return terms.findById(termId).map(Term::toInfo).orElseThrow(() -> new NotFoundException("Term not found"));
    }

    /** The term containing today; otherwise the latest past term; otherwise the next one. */
    @Transactional(readOnly = true)
    public Optional<TermInfo> currentTerm() {
        LocalDate today = LocalDate.now(clock);
        List<Term> ordered = terms.findAllOrdered();
        Optional<Term> current = ordered.stream().filter(term -> term.contains(today)).findFirst();
        if (current.isEmpty()) {
            current = ordered.stream().filter(term -> term.getEndDate().isBefore(today))
                    .max(Comparator.comparing(Term::getEndDate));
        }
        if (current.isEmpty()) {
            current = ordered.stream().filter(term -> term.getStartDate().isAfter(today)).findFirst();
        }
        return current.map(Term::toInfo);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> currentAcademicYearId() {
        return currentTerm().map(TermInfo::academicYearId);
    }

    private static void validateTerms(LocalDate yearStart, LocalDate yearEnd, List<NewTerm> newTerms) {
        if (newTerms == null || newTerms.isEmpty() || newTerms.size() > 3) {
            throw new BadRequestException("Give one to three terms");
        }
        Set<Integer> numbers = new HashSet<>();
        List<NewTerm> sorted = newTerms.stream().sorted(Comparator.comparing(NewTerm::startDate)).toList();
        LocalDate previousEnd = null;
        for (NewTerm term : sorted) {
            if (term.number() < 1 || term.number() > 3 || !numbers.add(term.number())) {
                throw new BadRequestException("Term numbers must be 1, 2 and 3, each used once");
            }
            if (!term.endDate().isAfter(term.startDate())) {
                throw new BadRequestException("Term " + term.number() + " must end after it starts");
            }
            if (term.startDate().isBefore(yearStart) || term.endDate().isAfter(yearEnd)) {
                throw new BadRequestException("Term " + term.number() + " must fall inside the academic year");
            }
            if (previousEnd != null && !term.startDate().isAfter(previousEnd)) {
                throw new BadRequestException("Terms must not overlap");
            }
            previousEnd = term.endDate();
        }
    }

    private static AcademicYearInfo toInfo(AcademicYear year, LocalDate today) {
        List<AcademicYearInfo.TermSummary> termSummaries = year.getTerms().stream()
                .map(term -> new AcademicYearInfo.TermSummary(term.getId(), term.getNumber(), term.getStartDate(),
                        term.getEndDate()))
                .toList();
        return new AcademicYearInfo(year.getId(), year.getYear(), year.getStartDate(), year.getEndDate(),
                year.contains(today), termSummaries);
    }
}
