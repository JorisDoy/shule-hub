package ke.shulehub.payments.internal;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import ke.shulehub.shared.config.TimeConfig;
import ke.shulehub.shared.web.GatewayException;

/**
 * Thin client for Safaricom Daraja (M-Pesa) APIs: OAuth token, STK Push, STK query and C2B URL
 * registration. Tokens are cached per consumer key until a minute before they expire.
 */
@Component
public class DarajaClient {

    public record Credentials(String baseUrl, String consumerKey, String consumerSecret) {
    }

    private record CachedToken(String value, Instant expiresAt) {
    }

    private static final Logger log = LoggerFactory.getLogger(DarajaClient.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RestClient http;
    private final Clock clock;
    private final Map<String, CachedToken> tokens = new ConcurrentHashMap<>();

    public DarajaClient(RestClient.Builder builder, Clock clock) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.http = builder.requestFactory(requestFactory).build();
        this.clock = clock;
    }

    /** Daraja's timestamp: East Africa Time, yyyyMMddHHmmss. */
    public String timestamp() {
        return LocalDateTime.now(clock.withZone(TimeConfig.NAIROBI)).format(TIMESTAMP);
    }

    /** Base64(shortcode + passkey + timestamp), as Daraja requires for STK requests. */
    public static String password(String shortcode, String passkey, String timestamp) {
        return Base64.getEncoder().encodeToString((shortcode + passkey + timestamp).getBytes(StandardCharsets.UTF_8));
    }

    public Daraja.StkPushResponse stkPush(Credentials credentials, Daraja.StkPushRequest request) {
        return post(credentials, "/mpesa/stkpush/v1/processrequest", request, Daraja.StkPushResponse.class);
    }

    public Daraja.StkQueryResponse stkQuery(Credentials credentials, Daraja.StkQueryRequest request) {
        return post(credentials, "/mpesa/stkpushquery/v1/query", request, Daraja.StkQueryResponse.class);
    }

    public Daraja.RegisterUrlResponse registerC2bUrls(Credentials credentials, Daraja.RegisterUrlRequest request) {
        return post(credentials, "/mpesa/c2b/v2/registerurl", request, Daraja.RegisterUrlResponse.class);
    }

    private <T> T post(Credentials credentials, String path, Object body, Class<T> responseType) {
        try {
            T response = http.post()
                    .uri(credentials.baseUrl() + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(credentials))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);
            if (response == null) {
                throw new GatewayException("M-Pesa returned an empty response");
            }
            return response;
        }
        catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401) {
                tokens.remove(credentials.consumerKey());
            }
            log.warn("Daraja {} failed: {} {}", path, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new GatewayException("M-Pesa rejected the request: " + errorMessage(ex), ex);
        }
        catch (RestClientException ex) {
            log.warn("Daraja {} unreachable: {}", path, ex.getMessage());
            throw new GatewayException("Could not reach M-Pesa. Try again in a moment", ex);
        }
    }

    private String accessToken(Credentials credentials) {
        CachedToken cached = tokens.get(credentials.consumerKey());
        Instant now = clock.instant();
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.value();
        }
        String basic = Base64.getEncoder().encodeToString(
                (credentials.consumerKey() + ":" + credentials.consumerSecret()).getBytes(StandardCharsets.UTF_8));
        Daraja.TokenResponse token;
        try {
            token = http.get()
                    .uri(credentials.baseUrl() + "/oauth/v1/generate?grant_type=client_credentials")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .retrieve()
                    .body(Daraja.TokenResponse.class);
        }
        catch (RestClientException ex) {
            throw new GatewayException("Could not sign in to M-Pesa; check the consumer key and secret", ex);
        }
        if (token == null || token.accessToken() == null) {
            throw new GatewayException("M-Pesa did not return an access token");
        }
        long seconds = parseSeconds(token.expiresIn());
        tokens.put(credentials.consumerKey(),
                new CachedToken(token.accessToken(), now.plusSeconds(Math.max(seconds - 60, 30))));
        return token.accessToken();
    }

    private static long parseSeconds(String expiresIn) {
        try {
            return expiresIn == null ? 3599 : Long.parseLong(expiresIn.trim());
        }
        catch (NumberFormatException ex) {
            return 3599;
        }
    }

    private static String errorMessage(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        int start = body.indexOf("\"errorMessage\"");
        if (start >= 0) {
            int colon = body.indexOf(':', start);
            int open = body.indexOf('"', colon + 1);
            int close = open < 0 ? -1 : body.indexOf('"', open + 1);
            if (open > 0 && close > open) {
                return body.substring(open + 1, close);
            }
        }
        return ex.getStatusText();
    }
}
