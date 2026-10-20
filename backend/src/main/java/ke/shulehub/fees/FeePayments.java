package ke.shulehub.fees;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ke.shulehub.identity.AccessPolicy;
import ke.shulehub.identity.ActorContext;
import ke.shulehub.identity.Permission;
import ke.shulehub.learners.LearnerDirectory;
import ke.shulehub.learners.LearnerSummary;
import ke.shulehub.payments.MpesaGateway;
import ke.shulehub.payments.MpesaGateway.StkInitiation;
import ke.shulehub.payments.MpesaGateway.StkRequest;
import ke.shulehub.payments.MpesaGateway.StkStatus;
import ke.shulehub.shared.web.NotFoundException;

/**
 * Asking a parent to pay by M-Pesa STK Push. The prompt goes to the learner's branch paybill
 * with the admission number as the account, so the money lands on the right ledger.
 */
@Service
public class FeePayments {

    private final LearnerDirectory learners;
    private final MpesaGateway gateway;
    private final AccessPolicy access;

    public FeePayments(LearnerDirectory learners, MpesaGateway gateway, AccessPolicy access) {
        this.learners = learners;
        this.gateway = gateway;
        this.access = access;
    }

    /** A bursar sends the prompt to a guardian's phone. */
    public StkInitiation requestByStaff(UUID learnerId, BigDecimal amount, String phone, UUID invoiceId,
            String idempotencyKey) {
        LearnerSummary learner = learners.require(learnerId);
        access.require(Permission.FEES_MANAGE, learner.branchId());
        return send(learner, amount, phone, invoiceId, idempotencyKey);
    }

    /** A guardian pays from the parent app; the portal has already checked guardianship. */
    public StkInitiation requestByGuardian(UUID learnerId, BigDecimal amount, String phone, String idempotencyKey) {
        return send(learners.require(learnerId), amount, phone, null, idempotencyKey);
    }

    public StkStatus statusForStaff(UUID intentId) {
        StkStatus status = gateway.status(intentId);
        LearnerSummary learner = learners.require(status.learnerId());
        access.require(Permission.FEES_VIEW, learner.branchId());
        return status;
    }

    public StkStatus statusForLearner(UUID learnerId, UUID intentId) {
        StkStatus status = gateway.status(intentId);
        if (!status.learnerId().equals(learnerId)) {
            throw new NotFoundException("Payment request not found");
        }
        return status;
    }

    private StkInitiation send(LearnerSummary learner, BigDecimal amount, String phone, UUID invoiceId,
            String idempotencyKey) {
        String key = idempotencyKey == null || idempotencyKey.isBlank() ? UUID.randomUUID().toString() : idempotencyKey;
        return gateway.initiateStk(new StkRequest(learner.branchId(), learner.id(), invoiceId, amount, phone,
                learner.admissionNumber(), ActorContext.current().map(actor -> actor.personId()).orElse(null), key));
    }
}
