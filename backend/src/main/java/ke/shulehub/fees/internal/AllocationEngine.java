package ke.shulehub.fees.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Decides how a payment is split across a learner's unpaid invoice lines. Pure: no database,
 * so the rules are unit-tested directly.
 *
 * <p>Order: the invoice the parent chose (if any) first, then the oldest due date, then the
 * oldest issue date; within an invoice, by vote-head priority (tuition before transport, for
 * example). Whatever is left over stays on the payment as credit.
 */
public final class AllocationEngine {

    public record OpenLine(UUID lineId, UUID invoiceId, LocalDate dueOn, LocalDate issuedOn, int priority,
            BigDecimal outstanding) {
    }

    public record Allocation(UUID lineId, UUID invoiceId, BigDecimal amount) {
    }

    private AllocationEngine() {
    }

    public static List<Allocation> allocate(BigDecimal available, List<OpenLine> lines, UUID preferredInvoiceId) {
        List<Allocation> allocations = new ArrayList<>();
        if (available == null || available.signum() <= 0) {
            return allocations;
        }
        Comparator<OpenLine> order = Comparator
                .comparing((OpenLine line) -> !line.invoiceId().equals(preferredInvoiceId))
                .thenComparing(OpenLine::dueOn)
                .thenComparing(OpenLine::issuedOn)
                .thenComparing(line -> line.invoiceId().toString())
                .thenComparingInt(OpenLine::priority)
                .thenComparing(line -> line.lineId().toString());
        BigDecimal remaining = available;
        for (OpenLine line : lines.stream().filter(line -> line.outstanding().signum() > 0).sorted(order).toList()) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal portion = remaining.min(line.outstanding());
            allocations.add(new Allocation(line.lineId(), line.invoiceId(), portion));
            remaining = remaining.subtract(portion);
        }
        return allocations;
    }
}
