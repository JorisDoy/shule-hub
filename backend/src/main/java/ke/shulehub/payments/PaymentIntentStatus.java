package ke.shulehub.payments;

/** PENDING: saved, not yet sent; SUBMITTED: prompt on the phone; then SUCCEEDED or FAILED. */
public enum PaymentIntentStatus {
    PENDING, SUBMITTED, SUCCEEDED, FAILED
}
