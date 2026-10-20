package ke.shulehub.fees.internal;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import ke.shulehub.shared.persistence.TenantScopedEntity;

/** An accounting bucket fees are billed and collected into (tuition, boarding, transport…). */
@Entity
@Table(name = "vote_head")
public class VoteHead extends TenantScopedEntity {

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    /** Lower numbers are paid first when a payment is split across vote heads. */
    @Column(name = "priority", nullable = false)
    private int priority;

    protected VoteHead() {
    }

    public VoteHead(String code, String name, int priority) {
        this.code = code.trim().toUpperCase(Locale.ROOT);
        this.name = name.trim();
        this.priority = priority;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }
}
