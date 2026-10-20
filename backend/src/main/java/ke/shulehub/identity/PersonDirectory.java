package ke.shulehub.identity;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.identity.internal.Person;
import ke.shulehub.identity.internal.PersonRepository;
import ke.shulehub.shared.contact.PhoneNumbers;
import ke.shulehub.shared.web.BadRequestException;

/** Finds and creates people. A phone number or email identifies a person across schools. */
@Service
public class PersonDirectory {

    private final PersonRepository people;

    public PersonDirectory(PersonRepository people) {
        this.people = people;
    }

    @Transactional(readOnly = true)
    public Optional<PersonRef> find(UUID personId) {
        return people.findById(personId).map(Person::toRef);
    }

    @Transactional(readOnly = true)
    public Map<UUID, PersonRef> findAll(Collection<UUID> personIds) {
        return people.findAllById(personIds).stream()
                .map(Person::toRef)
                .collect(Collectors.toMap(PersonRef::id, Function.identity()));
    }

    /**
     * Returns the person with this phone (or, failing that, email), creating them if needed.
     * Contact details are only added, never overwritten, so a typo can't hijack someone's record.
     */
    @Transactional
    public PersonRef findOrCreate(String givenName, String familyName, String phone, String email) {
        String normalizedPhone = phone == null || phone.isBlank() ? null : PhoneNumbers.require(phone, "Phone");
        String normalizedEmail = PhoneNumbers.normalizeEmail(email).orElse(null);
        if (normalizedPhone == null && normalizedEmail == null) {
            throw new BadRequestException("A phone number or email is needed to identify " + givenName);
        }
        Optional<Person> existing = Optional.ofNullable(normalizedPhone).flatMap(people::findByPhone)
                .or(() -> Optional.ofNullable(normalizedEmail).flatMap(people::findByEmail));
        if (existing.isPresent()) {
            Person person = existing.get();
            String phoneToAdd = normalizedPhone != null && people.findByPhone(normalizedPhone).isEmpty() ? normalizedPhone : null;
            String emailToAdd = normalizedEmail != null && people.findByEmail(normalizedEmail).isEmpty() ? normalizedEmail : null;
            person.fillMissingContact(phoneToAdd, emailToAdd);
            return person.toRef();
        }
        return people.save(new Person(givenName, familyName, normalizedPhone, normalizedEmail)).toRef();
    }
}
