package ke.shulehub.fees;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.fees.FeeViews.EntryType;
import ke.shulehub.fees.FeeViews.PaymentView;
import ke.shulehub.fees.FeeViews.Statement;
import ke.shulehub.fees.FeeViews.StatementEntry;
import ke.shulehub.fees.internal.AllocationEngine;
import ke.shulehub.fees.internal.Invoice;
import ke.shulehub.fees.internal.InvoiceLine;
import ke.shulehub.fees.internal.InvoiceRepository;
import ke.shulehub.fees.internal.Payment;
import ke.shulehub.fees.internal.PaymentAllocation;
import ke.shulehub.fees.internal.PaymentAllocationRepository;
import ke.shulehub.fees.internal.PaymentRepository;
import ke.shulehub.identity.AccessPolicy;
import ke.shulehub.identity.Permission;
import ke.shulehub.learners.LearnerDirectory;
import ke.shulehub.learners.LearnerSummary;
import ke.shulehub.shared.config.TimeConfig;
import ke.shulehub.shared.money.Money;
import ke.shulehub.shared.persistence.SequenceService;
import ke.shulehub.shared.web.BadRequestException;
import ke.shulehub.shared.web.NotFoundException;

/**
 * The money ledger: records payments exactly once, applies them to invoices, and reports
 * statements and balances. Work on one learner's ledger is serialised with a database lock.
 */
@Service
public class Ledger {

    public record NewPayment(UUID branchId, UUID learnerId, PaymentChannel channel, String reference,
            BigDecimal amount, String payerPhone, String payerName, String accountReference, Instant receivedAt,
            String rawPayload, UUID preferredInvoiceId) {
    }

    static final EnumSet<InvoiceStatus> UNPAID = EnumSet.of(InvoiceStatus.OPEN, InvoiceStatus.PARTIALLY_PAID);

    private static final Logger log = LoggerFactory.getLogger(Ledger.class);

    private final PaymentRepository payments;
    private final PaymentAllocationRepository allocations;
    private final InvoiceRepository invoices;
    private final LearnerDirectory learners;
    private final SequenceService locks;
    private final AccessPolicy access;
    private final Clock clock;

    public Ledger(PaymentRepository payments, PaymentAllocationRepository allocations, InvoiceRepository invoices,
            LearnerDirectory learners, SequenceService locks, AccessPolicy access, Clock clock) {
        this.payments = payments;
        this.allocations = allocations;
        this.invoices = invoices;
        this.learners = learners;
        this.locks = locks;
        this.access = access;
        this.clock = clock;
    }

    /**
     * Records a payment once (keyed by channel and reference) and applies it to the learner's
     * unpaid invoices. Returns the existing record when the same payment arrives again.
     */
    @Transactional
    public PaymentView record(NewPayment input) {
        if (!Money.isPositive(input.amount())) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }
        if (input.learnerId() != null) {
            locks.lockKey(input.learnerId());
        }
        Optional<Payment> existing = payments.findByDedupeKey(Payment.dedupeKey(input.channel(), input.reference()));
        if (existing.isPresent()) {
            log.info("Payment {} already recorded", input.reference());
            return view(existing.get());
        }
        Payment payment = payments.save(new Payment(input.branchId(), input.learnerId(), input.channel(),
                input.reference(), input.amount(), input.payerPhone(), input.payerName(), input.accountReference(),
                input.receivedAt(), input.rawPayload()));
        if (payment.getLearnerId() != null) {
            allocate(payment, input.preferredInvoiceId());
        }
        return view(payment);
    }

    /** Cash or bank-slip payment entered by the bursar. */
    @Transactional
    public PaymentView recordManual(UUID learnerId, BigDecimal amount, PaymentChannel channel, String reference,
            LocalDate receivedOn) {
        if (channel != PaymentChannel.CASH && channel != PaymentChannel.BANK) {
            throw new BadRequestException("Record M-Pesa payments through M-Pesa, not by hand");
        }
        LearnerSummary learner = learners.require(learnerId);
        access.require(Permission.FEES_MANAGE, learner.branchId());
        Instant receivedAt = (receivedOn == null ? LocalDate.now(clock) : receivedOn)
                .atStartOfDay(TimeConfig.NAIROBI).toInstant();
        return record(new NewPayment(learner.branchId(), learnerId, channel, reference, amount, null, null,
                learner.admissionNumber(), receivedAt, null, null));
    }

    /** Puts an unmatched (suspense) payment on a learner's account and applies it. */
    @Transactional
    public PaymentView assign(UUID paymentId, UUID learnerId) {
        Payment payment = payments.findById(paymentId).orElseThrow(() -> new NotFoundException("Payment not found"));
        access.require(Permission.FEES_MANAGE, payment.getBranchId());
        LearnerSummary learner = learners.require(learnerId);
        if (!learner.branchId().equals(payment.getBranchId())) {
            throw new BadRequestException("That learner is in a different branch from the paybill that received this");
        }
        locks.lockKey(learnerId);
        payment.assignTo(learnerId);
        allocate(payment, null);
        return view(payment);
    }

    /** Applies any credit the learner has to newly raised invoices. */
    @Transactional
    public void applyCredits(UUID learnerId) {
        locks.lockKey(learnerId);
        for (Payment payment : payments.findByLearnerIdAndStatusOrderByReceivedAtAsc(learnerId, PaymentStatus.ALLOCATED)) {
            if (payment.unallocated().signum() > 0) {
                allocate(payment, null);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentView> list(UUID branchId, PaymentStatus status, UUID learnerId) {
        access.require(Permission.FEES_VIEW, branchId);
        List<Payment> found;
        if (learnerId != null) {
            found = payments.findByLearnerIdOrderByReceivedAtAsc(learnerId).reversed().stream()
                    .filter(payment -> payment.getBranchId().equals(branchId))
                    .toList();
        }
        else if (status != null) {
            found = payments.findByBranchIdAndStatusOrderByReceivedAtDesc(branchId, status);
        }
        else {
            found = payments.findByBranchIdOrderByReceivedAtDesc(branchId);
        }
        Map<UUID, LearnerSummary> names = learners.findAll(found.stream()
                .map(Payment::getLearnerId).filter(Objects::nonNull).distinct().toList());
        return found.stream().map(payment -> view(payment, names.get(payment.getLearnerId()))).toList();
    }

    /** Statement for staff with fees access. */
    @Transactional(readOnly = true)
    public Statement statementForStaff(UUID learnerId) {
        LearnerSummary learner = learners.require(learnerId);
        access.require(Permission.FEES_VIEW, learner.branchId());
        return statement(learner);
    }

    /** Statement without a permission check; the parent portal checks guardianship first. */
    @Transactional(readOnly = true)
    public Statement statement(UUID learnerId) {
        return statement(learners.require(learnerId));
    }

    /** Positive: the family owes. Negative: credit. */
    @Transactional(readOnly = true)
    public BigDecimal balance(UUID learnerId) {
        BigDecimal invoiced = Money.sum(invoices.findByLearnerIdOrderByIssuedOnAsc(learnerId).stream()
                .filter(invoice -> invoice.getStatus() != InvoiceStatus.VOID).toList(), Invoice::getTotal);
        BigDecimal paid = Money.sum(payments.findByLearnerIdOrderByReceivedAtAsc(learnerId).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.ALLOCATED).toList(), Payment::getAmount);
        return invoiced.subtract(paid);
    }

    private Statement statement(LearnerSummary learner) {
        List<Invoice> learnerInvoices = invoices.findByLearnerIdOrderByIssuedOnAsc(learner.id()).stream()
                .filter(invoice -> invoice.getStatus() != InvoiceStatus.VOID).toList();
        List<Payment> learnerPayments = payments.findByLearnerIdOrderByReceivedAtAsc(learner.id()).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.ALLOCATED).toList();
        record Line(LocalDate date, int order, EntryType type, String reference, String description, BigDecimal debit,
                BigDecimal credit) {
        }
        List<Line> lines = new ArrayList<>();
        learnerInvoices.forEach(invoice -> lines.add(new Line(invoice.getIssuedOn(), 0, EntryType.INVOICE,
                invoice.getNumber(), invoice.getDescription(), invoice.getTotal(), Money.ZERO)));
        learnerPayments.forEach(payment -> lines.add(new Line(
                payment.getReceivedAt().atZone(TimeConfig.NAIROBI).toLocalDate(), 1, EntryType.PAYMENT,
                payment.getReference(), describe(payment.getChannel()), Money.ZERO, payment.getAmount())));
        lines.sort(Comparator.comparing(Line::date).thenComparingInt(Line::order));
        BigDecimal running = Money.ZERO;
        List<StatementEntry> entries = new ArrayList<>();
        for (Line line : lines) {
            running = running.add(line.debit()).subtract(line.credit());
            entries.add(new StatementEntry(line.date(), line.type(), line.reference(), line.description(), line.debit(),
                    line.credit(), running));
        }
        BigDecimal totalInvoiced = Money.sum(learnerInvoices, Invoice::getTotal);
        BigDecimal totalPaid = Money.sum(learnerPayments, Payment::getAmount);
        return new Statement(learner.id(), learner.fullName(), learner.admissionNumber(), totalInvoiced, totalPaid,
                totalInvoiced.subtract(totalPaid), entries);
    }

    private void allocate(Payment payment, UUID preferredInvoiceId) {
        List<Invoice> open = invoices.findWithLinesForLearner(payment.getLearnerId(), UNPAID);
        List<AllocationEngine.OpenLine> lines = new ArrayList<>();
        for (Invoice invoice : open) {
            for (InvoiceLine line : invoice.getLines()) {
                lines.add(new AllocationEngine.OpenLine(line.getId(), invoice.getId(), invoice.getDueOn(),
                        invoice.getIssuedOn(), line.getPriority(), line.outstanding()));
            }
        }
        Map<UUID, Invoice> byId = new java.util.HashMap<>();
        open.forEach(invoice -> byId.put(invoice.getId(), invoice));
        for (AllocationEngine.Allocation allocation : AllocationEngine.allocate(payment.unallocated(), lines,
                preferredInvoiceId)) {
            byId.get(allocation.invoiceId()).applyToLine(allocation.lineId(), allocation.amount());
            allocations.save(new PaymentAllocation(payment.getId(), allocation.lineId(), allocation.amount()));
            payment.recordAllocation(allocation.amount());
        }
    }

    private static String describe(PaymentChannel channel) {
        return switch (channel) {
            case MPESA_STK -> "M-Pesa (prompt)";
            case MPESA_C2B -> "M-Pesa (paybill)";
            case BANK -> "Bank deposit";
            case CASH -> "Cash";
        };
    }

    private PaymentView view(Payment payment) {
        LearnerSummary learner = payment.getLearnerId() == null ? null
                : learners.find(payment.getLearnerId()).orElse(null);
        return view(payment, learner);
    }

    private static PaymentView view(Payment payment, LearnerSummary learner) {
        return new PaymentView(payment.getId(), payment.getBranchId(), payment.getChannel(), payment.getReference(),
                payment.getAmount(), payment.getAmountAllocated(), payment.unallocated(), payment.getPayerPhone(),
                payment.getPayerName(), payment.getAccountReference(), payment.getLearnerId(),
                learner == null ? null : learner.fullName(), payment.getReceivedAt(), payment.getStatus());
    }
}
