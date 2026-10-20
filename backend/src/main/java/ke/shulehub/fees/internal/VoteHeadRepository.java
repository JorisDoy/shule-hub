package ke.shulehub.fees.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteHeadRepository extends JpaRepository<VoteHead, UUID> {

    List<VoteHead> findAllByOrderByPriorityAscNameAsc();

    boolean existsByCode(String code);
}
