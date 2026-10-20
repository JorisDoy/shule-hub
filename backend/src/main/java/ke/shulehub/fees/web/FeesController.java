package ke.shulehub.fees.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ke.shulehub.fees.ArrearsReport;
import ke.shulehub.fees.FeeAppliesTo;
import ke.shulehub.fees.FeePayments;
import ke.shulehub.fees.FeeSetup;
import ke.shulehub.fees.FeeViews.Arrears;
import ke.shulehub.fees.FeeViews.FeeStructureView;
import ke.shulehub.fees.FeeViews.GenerationResult;
import ke.shulehub.fees.FeeViews.InvoiceDetail;
import ke.shulehub.fees.FeeViews.InvoiceSummary;
import ke.shulehub.fees.FeeViews.PaymentView;
import ke.shulehub.fees.FeeViews.Statement;
import ke.shulehub.fees.FeeViews.VoteHeadView;
import ke.shulehub.fees.InvoiceStatus;
import ke.shulehub.fees.Invoicing;
import ke.shulehub.fees.Ledger;
import ke.shulehub.fees.PaymentChannel;
import ke.shulehub.fees.PaymentStatus;
import ke.shulehub.payments.MpesaGateway.StkInitiation;
import ke.shulehub.payments.MpesaGateway.StkStatus;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.tenancy.TenantContext;

@RestController
class FeesController {

    record CreateVoteHead(@NotBlank @Size(max = 20) String code, @NotBlank @Size(max = 60) String name,
            @Min(0) int priority) {
    }

    record ItemRequest(@NotNull UUID voteHeadId, @NotNull @DecimalMin("0.01") BigDecimal amount,
            FeeAppliesTo appliesTo) {
    }

    record StructureRequest(@NotNull UUID branchId, @NotNull UUID termId, @NotNull GradeLevel gradeLevel,
            @NotBlank @Size(max = 120) String name, @NotEmpty List<@Valid ItemRequest> items) {
    }

    record GenerateRequest(@NotNull UUID branchId, @NotNull UUID termId, @NotNull LocalDate dueOn) {
    }

    record ManualPaymentRequest(@NotNull UUID learnerId, @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull PaymentChannel channel, @NotBlank @Size(max = 60) String reference, LocalDate receivedOn) {
    }

    record AssignRequest(@NotNull UUID learnerId) {
    }

    record StkRequest(@NotNull UUID learnerId, @NotNull @DecimalMin("1") BigDecimal amount, @NotBlank String phone,
            UUID invoiceId) {
    }

    private final FeeSetup setup;
    private final Invoicing invoicing;
    private final Ledger ledger;
    private final ArrearsReport arrears;
    private final FeePayments feePayments;

    FeesController(FeeSetup setup, Invoicing invoicing, Ledger ledger, ArrearsReport arrears, FeePayments feePayments) {
        this.setup = setup;
        this.invoicing = invoicing;
        this.ledger = ledger;
        this.arrears = arrears;
        this.feePayments = feePayments;
    }

    @GetMapping("/api/fees/vote-heads")
    List<VoteHeadView> voteHeads() {
        TenantContext.require();
        return setup.voteHeads();
    }

    @PostMapping("/api/fees/vote-heads")
    @ResponseStatus(HttpStatus.CREATED)
    VoteHeadView createVoteHead(@Valid @RequestBody CreateVoteHead request) {
        TenantContext.require();
        return setup.createVoteHead(request.code(), request.name(), request.priority());
    }

    @GetMapping("/api/fees/structures")
    List<FeeStructureView> structures(@RequestParam UUID branchId, @RequestParam(required = false) UUID termId) {
        TenantContext.require();
        return setup.structures(branchId, termId);
    }

    @PostMapping("/api/fees/structures")
    @ResponseStatus(HttpStatus.CREATED)
    FeeStructureView createStructure(@Valid @RequestBody StructureRequest request) {
        TenantContext.require();
        return setup.create(request.branchId(), request.termId(), request.gradeLevel(), request.name(), items(request));
    }

    @PutMapping("/api/fees/structures/{id}")
    FeeStructureView updateStructure(@PathVariable UUID id, @Valid @RequestBody StructureRequest request) {
        TenantContext.require();
        return setup.update(id, request.name(), items(request));
    }

    @PostMapping("/api/fees/structures/{id}/publish")
    FeeStructureView publish(@PathVariable UUID id) {
        TenantContext.require();
        return setup.publish(id);
    }

    @PostMapping("/api/fees/invoices/generate")
    GenerationResult generate(@Valid @RequestBody GenerateRequest request) {
        TenantContext.require();
        return invoicing.generate(request.branchId(), request.termId(), request.dueOn());
    }

    @GetMapping("/api/fees/invoices")
    List<InvoiceSummary> invoices(@RequestParam UUID branchId, @RequestParam(required = false) UUID termId,
            @RequestParam(required = false) UUID learnerId, @RequestParam(required = false) InvoiceStatus status) {
        TenantContext.require();
        return invoicing.list(branchId, termId, learnerId, status);
    }

    @GetMapping("/api/fees/invoices/{id}")
    InvoiceDetail invoice(@PathVariable UUID id) {
        TenantContext.require();
        return invoicing.get(id);
    }

    @GetMapping("/api/fees/learners/{learnerId}/statement")
    Statement statement(@PathVariable UUID learnerId) {
        TenantContext.require();
        return ledger.statementForStaff(learnerId);
    }

    @GetMapping("/api/fees/arrears")
    Arrears arrears(@RequestParam UUID branchId, @RequestParam(required = false) LocalDate asOf) {
        TenantContext.require();
        return arrears.report(branchId, asOf);
    }

    @GetMapping("/api/fees/payments")
    List<PaymentView> payments(@RequestParam UUID branchId, @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) UUID learnerId) {
        TenantContext.require();
        return ledger.list(branchId, status, learnerId);
    }

    @PostMapping("/api/fees/payments")
    @ResponseStatus(HttpStatus.CREATED)
    PaymentView recordPayment(@Valid @RequestBody ManualPaymentRequest request) {
        TenantContext.require();
        return ledger.recordManual(request.learnerId(), request.amount(), request.channel(), request.reference(),
                request.receivedOn());
    }

    @PostMapping("/api/fees/payments/{id}/assign")
    PaymentView assign(@PathVariable UUID id, @Valid @RequestBody AssignRequest request) {
        TenantContext.require();
        return ledger.assign(id, request.learnerId());
    }

    @PostMapping("/api/fees/stk")
    StkInitiation stk(@Valid @RequestBody StkRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        TenantContext.require();
        return feePayments.requestByStaff(request.learnerId(), request.amount(), request.phone(), request.invoiceId(),
                idempotencyKey);
    }

    @GetMapping("/api/fees/stk/{intentId}")
    StkStatus stkStatus(@PathVariable UUID intentId) {
        TenantContext.require();
        return feePayments.statusForStaff(intentId);
    }

    private static List<FeeSetup.ItemInput> items(StructureRequest request) {
        return request.items().stream()
                .map(item -> new FeeSetup.ItemInput(item.voteHeadId(), item.amount(), item.appliesTo()))
                .toList();
    }
}
