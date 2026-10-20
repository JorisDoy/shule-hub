package ke.shulehub.fees;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.academics.AcademicCalendar;
import ke.shulehub.academics.TermInfo;
import ke.shulehub.fees.FeeViews.GenerationResult;
import ke.shulehub.fees.FeeViews.InvoiceDetail;
import ke.shulehub.fees.FeeViews.InvoiceLineView;
import ke.shulehub.fees.FeeViews.InvoiceSummary;
import ke.shulehub.fees.internal.FeeStructure;
import ke.shulehub.fees.internal.FeeStructureItem;
import ke.shulehub.fees.internal.FeeStructureRepository;
import ke.shulehub.fees.internal.Invoice;
import ke.shulehub.fees.internal.InvoiceRepository;
import ke.shulehub.fees.internal.VoteHead;
import ke.shulehub.fees.internal.VoteHeadRepository;
import ke.shulehub.identity.AccessPolicy;
import ke.shulehub.identity.Permission;
import ke.shulehub.learners.LearnerDirectory;
import ke.shulehub.learners.LearnerSummary;
import ke.shulehub.shared.persistence.SequenceService;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.web.BadRequestException;
import ke.shulehub.shared.web.NotFoundException;
import ke.shulehub.tenancy.BranchDirectory;

/** Raises term invoices from published fee structures. */
@Service
public class Invoicing {

    private final InvoiceRepository invoices;
    private final FeeStructureRepository structures;
    private final VoteHeadRepository voteHeads;
    private final LearnerDirectory learners;
    private final BranchDirectory branches;
    private final AcademicCalendar calendar;
    private final Ledger ledger;
    private final SequenceService sequences;
    private final AccessPolicy access;
    private final Clock clock;

    public Invoicing(InvoiceRepository invoices, FeeStructureRepository structures, VoteHeadRepository voteHeads,
            LearnerDirectory learners, BranchDirectory branches, AcademicCalendar calendar, Ledger ledger,
            SequenceService sequences, AccessPolicy access, Clock clock) {
        this.invoices = invoices;
        this.structures = structures;
        this.voteHeads = voteHeads;
        this.learners = learners;
        this.branches = branches;
        this.calendar = calendar;
        this.ledger = ledger;
        this.sequences = sequences;
        this.access = access;
        this.clock = clock;
    }

    /**
     * One invoice per active learner whose grade has a published structure for the term. Safe to
     * run again: learners already invoiced from that structure are skipped. Existing credit is
     * applied to the new invoices straight away.
     */
    @Transactional
    public GenerationResult generate(UUID branchId, UUID termId, LocalDate dueOn) {
        branches.require(branchId);
        access.require(Permission.FEES_MANAGE, branchId);
        TermInfo term = calendar.requireTerm(termId);
        if (dueOn == null) {
            throw new BadRequestException("Give a due date");
        }
        Map<GradeLevel, FeeStructure> byGrade = structures
                .findWithItemsByStatus(branchId, termId, FeeStructureStatus.PUBLISHED).stream()
                .collect(Collectors.toMap(FeeStructure::getGradeLevel, Function.identity()));
        if (byGrade.isEmpty()) {
            throw new BadRequestException("Publish a fee structure for this term first");
        }
        Map<UUID, VoteHead> heads = voteHeads.findAll().stream()
                .collect(Collectors.toMap(VoteHead::getId, Function.identity()));
        LocalDate today = LocalDate.now(clock);
        int created = 0;
        int skipped = 0;
        for (LearnerSummary learner : learners.activeInBranch(branchId)) {
            FeeStructure structure = byGrade.get(learner.gradeLevel());
            if (structure == null) {
                continue;
            }
            List<FeeStructureItem> items = structure.itemsFor(learner.boardingStatus());
            if (items.isEmpty() || invoices.existsByLearnerIdAndTermIdAndFeeStructureId(learner.id(), termId,
                    structure.getId())) {
                skipped++;
                continue;
            }
            String number = String.format(Locale.ROOT, "INV-%d-%06d", term.year(), sequences.next("INV-" + term.year()));
            Invoice invoice = new Invoice(branchId, learner.id(), termId, structure.getId(), number,
                    term.label() + " fees", today, dueOn);
            for (FeeStructureItem item : items) {
                VoteHead head = heads.get(item.getVoteHeadId());
                invoice.addLine(item.getVoteHeadId(), head.getName(), head.getPriority(), item.getAmount());
            }
            invoices.save(invoice);
            created++;
            ledger.applyCredits(learner.id());
        }
        return new GenerationResult(created, skipped);
    }

    @Transactional(readOnly = true)
    public List<InvoiceSummary> list(UUID branchId, UUID termId, UUID learnerId, InvoiceStatus status) {
        branches.require(branchId);
        access.require(Permission.FEES_VIEW, branchId);
        List<Invoice> found;
        if (learnerId != null) {
            found = invoices.findByLearnerIdOrderByIssuedOnAsc(learnerId).reversed().stream()
                    .filter(invoice -> invoice.getBranchId().equals(branchId))
                    .filter(invoice -> termId == null || invoice.getTermId().equals(termId))
                    .toList();
        }
        else if (termId != null) {
            found = invoices.findByBranchIdAndTermIdOrderByNumberAsc(branchId, termId);
        }
        else {
            found = invoices.findByBranchIdOrderByIssuedOnDescNumberDesc(branchId);
        }
        List<Invoice> filtered = status == null ? found
                : found.stream().filter(invoice -> invoice.getStatus() == status).toList();
        Map<UUID, LearnerSummary> names = learners.findAll(filtered.stream().map(Invoice::getLearnerId).distinct().toList());
        return filtered.stream().map(invoice -> summary(invoice, names.get(invoice.getLearnerId()))).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceDetail get(UUID invoiceId) {
        Invoice invoice = invoices.findWithLines(invoiceId).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        access.require(Permission.FEES_VIEW, invoice.getBranchId());
        LearnerSummary learner = learners.find(invoice.getLearnerId()).orElse(null);
        InvoiceSummary s = summary(invoice, learner);
        List<InvoiceLineView> lines = invoice.getLines().stream()
                .map(line -> new InvoiceLineView(line.getId(), line.getDescription(), line.getDescription(),
                        line.getAmount(), line.getAmountPaid()))
                .toList();
        return new InvoiceDetail(s.id(), s.number(), s.learnerId(), s.learnerName(), s.admissionNumber(), s.termId(),
                s.issuedOn(), s.dueOn(), s.total(), s.paid(), s.balance(), s.status(), lines);
    }

    static InvoiceSummary summary(Invoice invoice, LearnerSummary learner) {
        return new InvoiceSummary(invoice.getId(), invoice.getNumber(), invoice.getLearnerId(),
                learner == null ? null : learner.fullName(), learner == null ? null : learner.admissionNumber(),
                invoice.getTermId(), invoice.getIssuedOn(), invoice.getDueOn(), invoice.getTotal(),
                invoice.getAmountPaid(), invoice.balance(), invoice.getStatus());
    }
}
