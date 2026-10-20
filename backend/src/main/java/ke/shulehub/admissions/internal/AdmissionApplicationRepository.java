package ke.shulehub.admissions.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ke.shulehub.admissions.ApplicationStatus;

public interface AdmissionApplicationRepository extends JpaRepository<AdmissionApplication, UUID> {

    List<AdmissionApplication> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    List<AdmissionApplication> findByBranchIdAndStatusOrderByCreatedAtDesc(UUID branchId, ApplicationStatus status);
}
