package ke.shulehub.identity.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.identity.PersonRef;
import ke.shulehub.shared.contact.PhoneNumbers;

/**
 * Maps a Keycloak token to a person. People are created by schools (staff onboarding,
 * admissions), so a first login links by verified email or phone instead of creating anyone.
 */
@Component
public class ActorResolver {

    public static final String PLATFORM_ADMIN_ROLE = "platform-admin";

    private static final Logger log = LoggerFactory.getLogger(ActorResolver.class);

    private final PersonRepository people;

    public ActorResolver(PersonRepository people) {
        this.people = people;
    }

    @Transactional
    public Optional<PersonRef> resolve(Jwt jwt) {
        String subject = jwt.getSubject();
        Optional<Person> linked = people.findByIdpSubject(subject);
        if (linked.isPresent()) {
            return linked.map(Person::toRef);
        }
        Optional<Person> candidate = Optional.empty();
        if (Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            candidate = PhoneNumbers.normalizeEmail(jwt.getClaimAsString("email")).flatMap(people::findByEmail);
        }
        if (candidate.isEmpty() && Boolean.TRUE.equals(jwt.getClaimAsBoolean("phone_number_verified"))) {
            candidate = PhoneNumbers.normalize(jwt.getClaimAsString("phone_number")).flatMap(people::findByPhone);
        }
        candidate = candidate.filter(person -> person.getIdpSubject() == null);
        candidate.ifPresent(person -> {
            person.linkLogin(subject);
            log.info("Linked login {} to person {}", subject, person.getId());
        });
        return candidate.map(Person::toRef);
    }

    public static boolean isPlatformAdmin(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return false;
        }
        Object roles = realmAccess.get("roles");
        return roles instanceof Collection<?> collection && collection.contains(PLATFORM_ADMIN_ROLE);
    }
}
