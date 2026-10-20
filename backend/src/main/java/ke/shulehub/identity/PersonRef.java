package ke.shulehub.identity;

import java.util.UUID;

/** A person as other modules see them. */
public record PersonRef(UUID id, String givenName, String familyName, String phone, String email) {

    public String fullName() {
        return givenName + " " + familyName;
    }
}
