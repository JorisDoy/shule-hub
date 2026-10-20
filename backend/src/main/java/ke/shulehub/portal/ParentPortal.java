package ke.shulehub.portal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ke.shulehub.assessment.ReportCards;
import ke.shulehub.assessment.ReportCards.ReleasedTerm;
import ke.shulehub.assessment.ReportCards.ReportCardView;
import ke.shulehub.fees.FeePayments;
import ke.shulehub.fees.FeeViews.Statement;
import ke.shulehub.fees.Ledger;
import ke.shulehub.identity.ActorContext;
import ke.shulehub.identity.PersonRef;
import ke.shulehub.learners.LearnerDirectory;
import ke.shulehub.learners.LearnerSummary;
import ke.shulehub.payments.MpesaGateway.StkInitiation;
import ke.shulehub.payments.MpesaGateway.StkStatus;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.tenancy.TenantContext;
import ke.shulehub.shared.web.NotFoundException;

/**
 * What guardians can see and do: only for learners they are linked to. A learner who isn't
 * theirs looks exactly like one that doesn't exist (404).
 */
@Service
public class ParentPortal {

    public record Child(UUID learnerId, String fullName, String admissionNumber, GradeLevel gradeLevel,
            String streamName, BigDecimal balance) {
    }

    private final LearnerDirectory learners;
    private final Ledger ledger;
    private final ReportCards reportCards;
    private final FeePayments feePayments;

    public ParentPortal(LearnerDirectory learners, Ledger ledger, ReportCards reportCards, FeePayments feePayments) {
        this.learners = learners;
        this.ledger = ledger;
        this.reportCards = reportCards;
        this.feePayments = feePayments;
    }

    public List<Child> children() {
        TenantContext.require();
        PersonRef me = ActorContext.requirePerson();
        return learners.childrenOf(me.id()).stream()
                .map(child -> new Child(child.id(), child.fullName(), child.admissionNumber(), child.gradeLevel(),
                        child.streamName(), ledger.balance(child.id())))
                .toList();
    }

    public Statement statement(UUID learnerId) {
        requireMine(learnerId);
        return ledger.statement(learnerId);
    }

    public List<ReleasedTerm> reportCards(UUID learnerId) {
        requireMine(learnerId);
        return reportCards.releasedTerms(learnerId);
    }

    public ReportCardView reportCard(UUID learnerId, UUID termId) {
        requireMine(learnerId);
        return reportCards.released(learnerId, termId);
    }

    public StkInitiation pay(UUID learnerId, BigDecimal amount, String phone, String idempotencyKey) {
        requireMine(learnerId);
        return feePayments.requestByGuardian(learnerId, amount, phone, idempotencyKey);
    }

    public StkStatus paymentStatus(UUID learnerId, UUID intentId) {
        requireMine(learnerId);
        return feePayments.statusForLearner(learnerId, intentId);
    }

    private LearnerSummary requireMine(UUID learnerId) {
        TenantContext.require();
        PersonRef me = ActorContext.requirePerson();
        if (!learners.isGuardian(me.id(), learnerId)) {
            throw new NotFoundException("Learner not found");
        }
        return learners.require(learnerId);
    }
}
