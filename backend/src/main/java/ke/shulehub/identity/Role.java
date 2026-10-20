package ke.shulehub.identity;

import static ke.shulehub.identity.Permission.ACADEMICS_MANAGE;
import static ke.shulehub.identity.Permission.ADMISSIONS_MANAGE;
import static ke.shulehub.identity.Permission.ADMISSIONS_VIEW;
import static ke.shulehub.identity.Permission.ASSESSMENT_ENTER;
import static ke.shulehub.identity.Permission.ASSESSMENT_MANAGE;
import static ke.shulehub.identity.Permission.FEES_MANAGE;
import static ke.shulehub.identity.Permission.FEES_VIEW;
import static ke.shulehub.identity.Permission.LEARNERS_EDIT;
import static ke.shulehub.identity.Permission.LEARNERS_VIEW;
import static ke.shulehub.identity.Permission.PAYMENTS_CONFIGURE;
import static ke.shulehub.identity.Permission.REPORTS_RELEASE;

import java.util.EnumSet;
import java.util.Set;

/** School roles and the permissions each one carries. Branch management and tenant administration stay with the group director. */
public enum Role {
    GROUP_DIRECTOR(EnumSet.allOf(Permission.class)),
    BRANCH_HEAD(EnumSet.of(ACADEMICS_MANAGE, ADMISSIONS_VIEW, ADMISSIONS_MANAGE, LEARNERS_VIEW, LEARNERS_EDIT,
            FEES_VIEW, ASSESSMENT_ENTER, ASSESSMENT_MANAGE, REPORTS_RELEASE)),
    DEPUTY(EnumSet.of(ACADEMICS_MANAGE, ADMISSIONS_VIEW, LEARNERS_VIEW, ASSESSMENT_ENTER, ASSESSMENT_MANAGE,
            REPORTS_RELEASE)),
    BURSAR(EnumSet.of(FEES_VIEW, FEES_MANAGE, PAYMENTS_CONFIGURE, LEARNERS_VIEW)),
    ADMISSIONS_OFFICER(EnumSet.of(ADMISSIONS_VIEW, ADMISSIONS_MANAGE, LEARNERS_VIEW, LEARNERS_EDIT)),
    CLASS_TEACHER(EnumSet.of(LEARNERS_VIEW, ASSESSMENT_ENTER)),
    SUBJECT_TEACHER(EnumSet.of(LEARNERS_VIEW, ASSESSMENT_ENTER)),
    /** Guardians act through the parent portal, which checks guardianship instead of permissions. */
    PARENT(EnumSet.noneOf(Permission.class));

    private final EnumSet<Permission> permissions;

    Role(EnumSet<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> permissions() {
        return EnumSet.copyOf(permissions);
    }

    public boolean grants(Permission permission) {
        return permissions.contains(permission);
    }
}
