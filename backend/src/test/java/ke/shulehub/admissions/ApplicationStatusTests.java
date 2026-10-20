package ke.shulehub.admissions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApplicationStatusTests {

    @Test
    void followsTheAdmissionsWorkflow() {
        assertThat(ApplicationStatus.ENQUIRY.canMoveTo(ApplicationStatus.APPLIED)).isTrue();
        assertThat(ApplicationStatus.APPLIED.canMoveTo(ApplicationStatus.OFFERED)).isTrue();
        assertThat(ApplicationStatus.OFFERED.canMoveTo(ApplicationStatus.ACCEPTED)).isTrue();
        assertThat(ApplicationStatus.ENQUIRY.canMoveTo(ApplicationStatus.ACCEPTED)).isFalse();
    }

    @Test
    void admissionIsOnlyReachedThroughTheAdmitAction() {
        for (ApplicationStatus status : ApplicationStatus.values()) {
            assertThat(status.allowedTransitions()).doesNotContain(ApplicationStatus.ADMITTED);
        }
    }

    @Test
    void finalStatusesGoNowhere() {
        assertThat(ApplicationStatus.ADMITTED.isFinal()).isTrue();
        assertThat(ApplicationStatus.REJECTED.allowedTransitions()).isEmpty();
        assertThat(ApplicationStatus.WITHDRAWN.isFinal()).isTrue();
    }
}
