package ke.shulehub.tenancy.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    List<Branch> findAllByOrderByNameAsc();

    Optional<Branch> findByCode(String code);

    boolean existsByCode(String code);
}
