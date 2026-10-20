package ke.shulehub.shared.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.shared.tenancy.TenantContext;

/**
 * Gap-tolerant, concurrency-safe counters per tenant (admission numbers, invoice numbers).
 * One atomic upsert; the first call for a name returns 1.
 */
@Service
public class SequenceService {

    private static final String NEXT_VALUE = """
            insert into number_sequence (tenant_id, name, next_value) values (?, ?, 2)
            on conflict (tenant_id, name) do update set next_value = number_sequence.next_value + 1
            returning next_value - 1
            """;

    private final JdbcTemplate jdbc;

    public SequenceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public long next(String name) {
        UUID tenantId = TenantContext.require();
        Long value = jdbc.queryForObject(NEXT_VALUE, Long.class, tenantId, name);
        if (value == null) {
            throw new IllegalStateException("Sequence " + name + " returned no value");
        }
        return value;
    }

    /** Serialises work on one learner's ledger for the rest of the current transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockKey(UUID key) {
        long lockId = key.getMostSignificantBits() ^ key.getLeastSignificantBits();
        jdbc.queryForObject("select 1 from (select pg_advisory_xact_lock(?)) as locked", Integer.class, lockId);
    }
}
