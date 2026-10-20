package ke.shulehub.fees;

/** ALLOCATED: tied to a learner (any unallocated part is credit); SUSPENSE: account number matched nobody. */
public enum PaymentStatus {
    ALLOCATED, SUSPENSE, REVERSED
}
