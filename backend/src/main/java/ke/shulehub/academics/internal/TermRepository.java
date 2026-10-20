package ke.shulehub.academics.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TermRepository extends JpaRepository<Term, UUID> {

    @Query("select t from Term t join fetch t.academicYear order by t.startDate asc")
    List<Term> findAllOrdered();
}
