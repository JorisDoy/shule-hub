package ke.shulehub.identity.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {

    /** Filtered to the current tenant by Hibernate (or all tenants under the root context). */
    List<RoleAssignment> findByPersonId(UUID personId);
}
