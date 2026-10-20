package ke.shulehub.admissions;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Where an application stands. ADMITTED is reached only through the admit action. */
public enum ApplicationStatus {
    ENQUIRY,
    APPLIED,
    ASSESSMENT_SCHEDULED,
    OFFERED,
    ACCEPTED,
    ADMITTED,
    WAITLISTED,
    REJECTED,
    WITHDRAWN;

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> NEXT = Map.of(
            ENQUIRY, EnumSet.of(APPLIED, REJECTED, WITHDRAWN),
            APPLIED, EnumSet.of(ASSESSMENT_SCHEDULED, OFFERED, WAITLISTED, REJECTED, WITHDRAWN),
            ASSESSMENT_SCHEDULED, EnumSet.of(OFFERED, WAITLISTED, REJECTED, WITHDRAWN),
            WAITLISTED, EnumSet.of(OFFERED, REJECTED, WITHDRAWN),
            OFFERED, EnumSet.of(ACCEPTED, REJECTED, WITHDRAWN),
            ACCEPTED, EnumSet.of(WITHDRAWN),
            ADMITTED, EnumSet.noneOf(ApplicationStatus.class),
            REJECTED, EnumSet.noneOf(ApplicationStatus.class),
            WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class));

    /** Statuses a person can move this application to by hand (excludes ADMITTED). */
    public Set<ApplicationStatus> allowedTransitions() {
        Set<ApplicationStatus> next = NEXT.get(this);
        return next.isEmpty() ? EnumSet.noneOf(ApplicationStatus.class) : EnumSet.copyOf(next);
    }

    public boolean canMoveTo(ApplicationStatus target) {
        return NEXT.get(this).contains(target);
    }

    public boolean isFinal() {
        return NEXT.get(this).isEmpty();
    }
}
