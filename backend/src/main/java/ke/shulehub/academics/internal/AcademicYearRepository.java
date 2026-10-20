package ke.shulehub.academics.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    @Query("select distinct y from AcademicYear y left join fetch y.terms order by y.year desc")
    List<AcademicYear> findAllWithTerms();

    boolean existsByYear(int year);
}
