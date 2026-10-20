package ke.shulehub.fees;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import ke.shulehub.shared.school.GradeLevel;

/** JSON shapes of the fees API (see docs/api.md). */
public final class FeeViews {

    private FeeViews() {
    }

    public record VoteHeadView(UUID id, String code, String name, int priority) {
    }

    public record FeeItemView(UUID id, UUID voteHeadId, String voteHeadName, BigDecimal amount, FeeAppliesTo appliesTo) {
    }

    public record FeeStructureView(UUID id, UUID branchId, UUID termId, GradeLevel gradeLevel, String name,
            FeeStructureStatus status, BigDecimal dayTotal, BigDecimal boardingTotal, List<FeeItemView> items) {
    }

    public record GenerationResult(int created, int skipped) {
    }

    public record InvoiceSummary(UUID id, String number, UUID learnerId, String learnerName, String admissionNumber,
            UUID termId, LocalDate issuedOn, LocalDate dueOn, BigDecimal total, BigDecimal paid, BigDecimal balance,
            InvoiceStatus status) {
    }

    public record InvoiceLineView(UUID id, String voteHeadName, String description, BigDecimal amount, BigDecimal paid) {
    }

    public record InvoiceDetail(UUID id, String number, UUID learnerId, String learnerName, String admissionNumber,
            UUID termId, LocalDate issuedOn, LocalDate dueOn, BigDecimal total, BigDecimal paid, BigDecimal balance,
            InvoiceStatus status, List<InvoiceLineView> lines) {
    }

    public record PaymentView(UUID id, UUID branchId, PaymentChannel channel, String reference, BigDecimal amount,
            BigDecimal allocated, BigDecimal unallocated, String payerPhone, String payerName, String accountReference,
            UUID learnerId, String learnerName, Instant receivedAt, PaymentStatus status) {
    }

    public enum EntryType {
        INVOICE,
        PAYMENT
    }

    public record StatementEntry(LocalDate date, EntryType type, String reference, String description,
            BigDecimal debit, BigDecimal credit, BigDecimal runningBalance) {
    }

    public record Statement(UUID learnerId, String learnerName, String admissionNumber, BigDecimal totalInvoiced,
            BigDecimal totalPaid, BigDecimal balance, List<StatementEntry> entries) {
    }

    public record AgingBuckets(BigDecimal notDue, BigDecimal days1To30, BigDecimal days31To60,
            BigDecimal days61To90, BigDecimal over90, BigDecimal total) {
    }

    public record ArrearsRow(UUID learnerId, String learnerName, String admissionNumber, GradeLevel gradeLevel,
            String primaryContactPhone, BigDecimal notDue, BigDecimal days1To30, BigDecimal days31To60,
            BigDecimal days61To90, BigDecimal over90, BigDecimal total, LocalDate oldestDueOn) {
    }

    public record Arrears(LocalDate asOf, AgingBuckets totals, List<ArrearsRow> learners) {
    }
}
