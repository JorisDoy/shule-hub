package ke.shulehub.portal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ke.shulehub.assessment.ReportCards.ReleasedTerm;
import ke.shulehub.assessment.ReportCards.ReportCardView;
import ke.shulehub.fees.FeeViews.Statement;
import ke.shulehub.payments.MpesaGateway.StkInitiation;
import ke.shulehub.payments.MpesaGateway.StkStatus;
import ke.shulehub.portal.ParentPortal.Child;

@RestController
@RequestMapping("/api/me/children")
class ParentController {

    record PayRequest(@NotNull @DecimalMin("1") BigDecimal amount, @NotBlank String phone) {
    }

    private final ParentPortal portal;

    ParentController(ParentPortal portal) {
        this.portal = portal;
    }

    @GetMapping
    List<Child> children() {
        return portal.children();
    }

    @GetMapping("/{learnerId}/statement")
    Statement statement(@PathVariable UUID learnerId) {
        return portal.statement(learnerId);
    }

    @GetMapping("/{learnerId}/report-cards")
    List<ReleasedTerm> reportCards(@PathVariable UUID learnerId) {
        return portal.reportCards(learnerId);
    }

    @GetMapping("/{learnerId}/report-cards/{termId}")
    ReportCardView reportCard(@PathVariable UUID learnerId, @PathVariable UUID termId) {
        return portal.reportCard(learnerId, termId);
    }

    @PostMapping("/{learnerId}/pay")
    StkInitiation pay(@PathVariable UUID learnerId, @Valid @RequestBody PayRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return portal.pay(learnerId, request.amount(), request.phone(), idempotencyKey);
    }

    @GetMapping("/{learnerId}/payments/{intentId}")
    StkStatus payment(@PathVariable UUID learnerId, @PathVariable UUID intentId) {
        return portal.paymentStatus(learnerId, intentId);
    }
}
