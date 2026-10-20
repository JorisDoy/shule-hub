package ke.shulehub.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ke.shulehub.shared.contact.PhoneNumbers;

class PhoneNumbersTests {

    @Test
    void normalisesTheWaysKenyansWriteMobileNumbers() {
        assertThat(PhoneNumbers.normalize("0712 345 678")).hasValue("254712345678");
        assertThat(PhoneNumbers.normalize("+254712345678")).hasValue("254712345678");
        assertThat(PhoneNumbers.normalize("254712345678")).hasValue("254712345678");
        assertThat(PhoneNumbers.normalize("712-345-678")).hasValue("254712345678");
        assertThat(PhoneNumbers.normalize("0110123456")).hasValue("254110123456");
    }

    @Test
    void rejectsNumbersThatAreNotKenyanMobiles() {
        assertThat(PhoneNumbers.normalize("020 123 4567")).isEmpty();
        assertThat(PhoneNumbers.normalize("+255712345678")).isEmpty();
        assertThat(PhoneNumbers.normalize("07123")).isEmpty();
        assertThat(PhoneNumbers.normalize(null)).isEmpty();
    }
}
