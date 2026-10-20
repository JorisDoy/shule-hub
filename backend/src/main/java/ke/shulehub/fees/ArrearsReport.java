package ke.shulehub.fees;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.fees.FeeViews.AgingBuckets;
import ke.shulehub.fees.FeeViews.Arrears;
import ke.shulehub.fees.FeeViews.ArrearsRow;
import ke.shulehub.fees.internal.Invoice;
import ke.shulehub.fees.internal.InvoiceRepository;
import ke.shulehub.identity.AccessPolicy;
import ke.shulehub.identity.Permission;
import ke.shulehub.learners.LearnerDirectory;
import ke.shulehub.learners.LearnerSummary;
import ke.shulehub.shared.money.Money;
import ke.shulehub.tenancy.BranchDirectory;

/** Who owes what, aged by how long past the due date. */
@Service
public class ArrearsReport {

    private final InvoiceRepository invoices;
    private final LearnerDirectory learners;
    private final BranchDirectory branches;
    private final AccessPolicy access;
    private final Clock clock;

    public ArrearsReport(InvoiceRepository invoices, LearnerDirectory learners, BranchDirectory branches,
            AccessPolicy access, Clock clock) {
        this.invoices = invoices;
        this.learners = learners;
        this.branches = branches;
        this.access = access;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Arrears report(UUID branchId, LocalDate asOf) {
        branches.require(branchId);
        access.require(Permission.FEES_VIEW, branchId);
        LocalDate date = asOf == null ? LocalDate.now(clock) : asOf;
        Map<UUID, BigDecimal[]> byLearner = new LinkedHashMap<>();
        Map<UUID, LocalDate> oldestDue = new LinkedHashMap<>();
        for (Invoice invoice : invoices.findByBranchIdAndStatusIn(branchId, Ledger.UNPAID)) {
            BigDecimal outstanding = invoice.balance();
            if (outstanding.signum() <= 0 || invoice.getIssuedOn().isAfter(date)) {
                continue;
            }
            BigDecimal[] buckets = byLearner.computeIfAbsent(invoice.getLearnerId(), id -> zeros());
            buckets[bucket(invoice.getDueOn(), date)] = buckets[bucket(invoice.getDueOn(), date)].add(outstanding);
            oldestDue.merge(invoice.getLearnerId(), invoice.getDueOn(), (a, b) -> a.isBefore(b) ? a : b);
        }
        Map<UUID, LearnerSummary> summaries = learners.findAll(byLearner.keySet());
        List<ArrearsRow> rows = new ArrayList<>();
        BigDecimal[] totals = zeros();
        byLearner.forEach((learnerId, buckets) -> {
            LearnerSummary learner = summaries.get(learnerId);
            for (int i = 0; i < buckets.length; i++) {
                totals[i] = totals[i].add(buckets[i]);
            }
            rows.add(new ArrearsRow(learnerId, learner == null ? null : learner.fullName(),
                    learner == null ? null : learner.admissionNumber(), learner == null ? null : learner.gradeLevel(),
                    learner == null ? null : learner.primaryContactPhone(), buckets[0], buckets[1], buckets[2],
                    buckets[3], buckets[4], sum(buckets), oldestDue.get(learnerId)));
        });
        rows.sort(Comparator.comparing(ArrearsRow::total).reversed());
        return new Arrears(date, new AgingBuckets(totals[0], totals[1], totals[2], totals[3], totals[4], sum(totals)),
                rows);
    }

    /** 0 = not yet due, 1 = 1–30 days late, 2 = 31–60, 3 = 61–90, 4 = over 90. */
    static int bucket(LocalDate dueOn, LocalDate asOf) {
        long late = ChronoUnit.DAYS.between(dueOn, asOf);
        if (late <= 0) {
            return 0;
        }
        if (late <= 30) {
            return 1;
        }
        if (late <= 60) {
            return 2;
        }
        return late <= 90 ? 3 : 4;
    }

    private static BigDecimal[] zeros() {
        return new BigDecimal[] { Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO };
    }

    private static BigDecimal sum(BigDecimal[] values) {
        BigDecimal total = Money.ZERO;
        for (BigDecimal value : values) {
            total = total.add(value);
        }
        return total;
    }
}
