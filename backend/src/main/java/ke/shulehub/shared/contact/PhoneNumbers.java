package ke.shulehub.shared.contact;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import ke.shulehub.shared.web.BadRequestException;

/** Kenyan mobile numbers in the 2547XXXXXXXX / 2541XXXXXXXX form M-Pesa expects. */
public final class PhoneNumbers {

    private static final Pattern CANONICAL = Pattern.compile("^254[17]\\d{8}$");
    private static final Pattern LOCAL = Pattern.compile("^0[17]\\d{8}$");
    private static final Pattern SHORT = Pattern.compile("^[17]\\d{8}$");

    private PhoneNumbers() {
    }

    /** Accepts 07…, 01…, 7…, +254…, 254…, with spaces or dashes; empty if not a Kenyan mobile. */
    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String digits = raw.replaceAll("[\\s\\-().]", "");
        if (digits.startsWith("+")) {
            digits = digits.substring(1);
        }
        if (CANONICAL.matcher(digits).matches()) {
            return Optional.of(digits);
        }
        if (LOCAL.matcher(digits).matches()) {
            return Optional.of("254" + digits.substring(1));
        }
        if (SHORT.matcher(digits).matches()) {
            return Optional.of("254" + digits);
        }
        return Optional.empty();
    }

    public static String require(String raw, String field) {
        return normalize(raw).orElseThrow(
                () -> new BadRequestException(field + " must be a Kenyan mobile number such as 0712 345 678"));
    }

    /** Lower-cases and trims an email; empty for blank input. */
    public static Optional<String> normalizeEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(raw.trim().toLowerCase(Locale.ROOT));
    }
}
