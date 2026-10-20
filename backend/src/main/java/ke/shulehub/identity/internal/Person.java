package ke.shulehub.identity.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import ke.shulehub.identity.PersonRef;
import ke.shulehub.shared.persistence.BaseEntity;

/** One human across all tenants: staff, guardians and (later) learners with logins. */
@Entity
@Table(name = "person")
public class Person extends BaseEntity {

    @Column(name = "given_name", nullable = false)
    private String givenName;

    @Column(name = "family_name", nullable = false)
    private String familyName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "idp_subject")
    private String idpSubject;

    protected Person() {
    }

    public Person(String givenName, String familyName, String phone, String email) {
        this.givenName = givenName.trim();
        this.familyName = familyName.trim();
        this.phone = phone;
        this.email = email;
    }

    public PersonRef toRef() {
        return new PersonRef(getId(), givenName, familyName, phone, email);
    }

    public void linkLogin(String subject) {
        this.idpSubject = subject;
    }

    public void fillMissingContact(String newPhone, String newEmail) {
        if (phone == null && newPhone != null) {
            phone = newPhone;
        }
        if (email == null && newEmail != null) {
            email = newEmail;
        }
    }

    public String getIdpSubject() {
        return idpSubject;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }
}
