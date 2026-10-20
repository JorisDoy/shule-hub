package ke.shulehub.payments.internal;

import java.time.Clock;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Stores every raw Safaricom callback before it is processed, for audit and replay. */
@Component
public class CallbackLog {

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public CallbackLog(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public UUID record(String kind, String callbackToken, String payload) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into mpesa_callback_log (id, received_at, kind, callback_token, payload) values (?, ?, ?, ?, ?)",
                id, java.sql.Timestamp.from(clock.instant()), kind, callbackToken, payload);
        return id;
    }

    public void outcome(UUID id, UUID tenantId, String outcome) {
        jdbc.update("update mpesa_callback_log set tenant_id = ?, outcome = ? where id = ?", tenantId, outcome, id);
    }
}
