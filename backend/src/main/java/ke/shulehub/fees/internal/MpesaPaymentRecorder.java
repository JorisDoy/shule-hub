package ke.shulehub.fees.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import ke.shulehub.fees.Ledger;
import ke.shulehub.fees.PaymentChannel;
import ke.shulehub.payments.MpesaPaymentReceived;
import ke.shulehub.shared.tenancy.TenantContext;

/**
 * Posts confirmed M-Pesa payments to the fees ledger after the callback transaction commits.
 * The tenant is bound before the ledger's transaction opens. Failures leave the event
 * incomplete in the publication registry, which re-delivers it on restart.
 */
@Component
public class MpesaPaymentRecorder {

    private static final Logger log = LoggerFactory.getLogger(MpesaPaymentRecorder.class);

    private final Ledger ledger;

    public MpesaPaymentRecorder(Ledger ledger) {
        this.ledger = ledger;
    }

    @Async
    @TransactionalEventListener
    public void on(MpesaPaymentReceived event) {
        TenantContext.runAs(event.tenantId(), () -> {
            ledger.record(new Ledger.NewPayment(event.branchId(), event.learnerId(),
                    event.source() == MpesaPaymentReceived.Source.STK ? PaymentChannel.MPESA_STK
                            : PaymentChannel.MPESA_C2B,
                    event.receipt(), event.amount(), event.payerPhone(), event.payerName(), event.accountReference(),
                    event.receivedAt(), event.rawPayload(), event.invoiceId()));
            log.info("Recorded M-Pesa payment {} of KES {}", event.receipt(), event.amount());
        });
    }
}
