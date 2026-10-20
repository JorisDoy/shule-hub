package ke.shulehub.identity;

/** Fine-grained actions. Roles bundle permissions; grants bound them to a scope. */
public enum Permission {
    TENANT_ADMIN,
    BRANCH_MANAGE,
    ACADEMICS_MANAGE,
    ADMISSIONS_VIEW,
    ADMISSIONS_MANAGE,
    LEARNERS_VIEW,
    LEARNERS_EDIT,
    FEES_VIEW,
    FEES_MANAGE,
    PAYMENTS_CONFIGURE,
    ASSESSMENT_ENTER,
    ASSESSMENT_MANAGE,
    REPORTS_RELEASE
}
