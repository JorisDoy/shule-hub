package ke.shulehub.fees.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ke.shulehub.fees.internal.AllocationEngine.Allocation;
import ke.shulehub.fees.internal.AllocationEngine.OpenLine;

class AllocationEngineTests {

    private final UUID oldInvoice = UUID.randomUUID();
    private final UUID newInvoice = UUID.randomUUID();
    private final OpenLine oldTuition = line(oldInvoice, "2027-01-31", 10, "10000");
    private final OpenLine oldTransport = line(oldInvoice, "2027-01-31", 40, "3000");
    private final OpenLine newTuition = line(newInvoice, "2027-05-31", 10, "12000");

    @Test
    void paysTheOldestInvoiceFirstAndTuitionBeforeTransport() {
        List<Allocation> result = AllocationEngine.allocate(new BigDecimal("11000"),
                List.of(newTuition, oldTransport, oldTuition), null);

        assertThat(result).extracting(Allocation::lineId).containsExactly(oldTuition.lineId(), oldTransport.lineId());
        assertThat(result).extracting(Allocation::amount)
                .containsExactly(new BigDecimal("10000"), new BigDecimal("1000"));
    }

    @Test
    void honoursTheInvoiceTheParentChose() {
        List<Allocation> result = AllocationEngine.allocate(new BigDecimal("5000"),
                List.of(oldTuition, oldTransport, newTuition), newInvoice);

        assertThat(result).singleElement().satisfies(allocation -> {
            assertThat(allocation.lineId()).isEqualTo(newTuition.lineId());
            assertThat(allocation.amount()).isEqualByComparingTo("5000");
        });
    }

    @Test
    void leavesTheRemainderAsCredit() {
        List<Allocation> result = AllocationEngine.allocate(new BigDecimal("30000"),
                List.of(oldTuition, oldTransport, newTuition), null);

        BigDecimal applied = result.stream().map(Allocation::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(applied).isEqualByComparingTo("25000");
    }

    @Test
    void ignoresPaidLinesAndZeroPayments() {
        OpenLine paid = line(oldInvoice, "2027-01-31", 5, "0");
        assertThat(AllocationEngine.allocate(new BigDecimal("100"), List.of(paid), null)).isEmpty();
        assertThat(AllocationEngine.allocate(BigDecimal.ZERO, List.of(oldTuition), null)).isEmpty();
    }

    private static OpenLine line(UUID invoiceId, String dueOn, int priority, String outstanding) {
        return new OpenLine(UUID.randomUUID(), invoiceId, LocalDate.parse(dueOn), LocalDate.parse("2027-01-05"), priority,
                new BigDecimal(outstanding));
    }
}
