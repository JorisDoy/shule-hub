package ke.shulehub.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class GrantTests {

    private final UUID branch = UUID.randomUUID();
    private final UUID otherBranch = UUID.randomUUID();
    private final UUID stream = UUID.randomUUID();

    @Test
    void groupGrantsCoverEveryBranch() {
        Grant director = Grant.group(Role.GROUP_DIRECTOR);
        assertThat(director.allows(Permission.FEES_MANAGE, branch, null)).isTrue();
        assertThat(director.allows(Permission.BRANCH_MANAGE, null, null)).isTrue();
    }

    @Test
    void branchGrantsStayInTheirBranch() {
        Grant bursar = Grant.branch(Role.BURSAR, branch);
        assertThat(bursar.allows(Permission.FEES_MANAGE, branch, null)).isTrue();
        assertThat(bursar.allows(Permission.FEES_MANAGE, otherBranch, null)).isFalse();
        assertThat(bursar.allows(Permission.FEES_MANAGE, null, null)).isFalse();
        assertThat(bursar.allows(Permission.ADMISSIONS_MANAGE, branch, null)).isFalse();
    }

    @Test
    void classTeachersOnlyActOnTheirStream() {
        Grant teacher = Grant.stream(Role.CLASS_TEACHER, branch, stream);
        assertThat(teacher.allows(Permission.ASSESSMENT_ENTER, branch, stream)).isTrue();
        assertThat(teacher.allows(Permission.ASSESSMENT_ENTER, branch, UUID.randomUUID())).isFalse();
        assertThat(teacher.allows(Permission.ASSESSMENT_ENTER, branch, null)).isFalse();
        assertThat(teacher.allows(Permission.REPORTS_RELEASE, branch, stream)).isFalse();
    }

    @Test
    void parentsHaveNoStaffPermissions() {
        assertThat(Role.PARENT.permissions()).isEmpty();
    }
}
