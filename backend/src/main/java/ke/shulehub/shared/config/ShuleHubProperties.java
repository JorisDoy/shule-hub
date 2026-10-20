package ke.shulehub.shared.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application settings under {@code shulehub.*}.
 *
 * @param credentialsKey base64 AES-256 key that encrypts stored M-Pesa credentials
 * @param cors browser origins allowed to call the API
 * @param mpesa Daraja endpoints and the public base URL Safaricom calls back on
 * @param demo whether to seed the demo school on start-up
 */
@ConfigurationProperties("shulehub")
public record ShuleHubProperties(
        String credentialsKey,
        @DefaultValue Cors cors,
        @DefaultValue Mpesa mpesa,
        @DefaultValue Demo demo) {

    public record Cors(@DefaultValue("http://localhost:3000") List<String> allowedOrigins) {
    }

    public record Mpesa(
            @DefaultValue("http://localhost:8080") String callbackBaseUrl,
            @DefaultValue("https://sandbox.safaricom.co.ke") String sandboxBaseUrl,
            @DefaultValue("https://api.safaricom.co.ke") String productionBaseUrl,
            @DefaultValue("true") boolean rejectUnknownAccounts) {
    }

    public record Demo(@DefaultValue("false") boolean enabled) {
    }
}
