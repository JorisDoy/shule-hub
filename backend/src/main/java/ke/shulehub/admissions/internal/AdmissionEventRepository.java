package ke.shulehub.admissions.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionEventRepository extends JpaRepository<AdmissionEvent, UUID> {

    List<AdmissionEvent> findByApplicationIdOrderByOccurredAtAsc(UUID applicationId);
}
