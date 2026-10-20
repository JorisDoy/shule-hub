package ke.shulehub.identity.internal;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, UUID> {

    Optional<Person> findByIdpSubject(String idpSubject);

    Optional<Person> findByEmail(String email);

    Optional<Person> findByPhone(String phone);
}
