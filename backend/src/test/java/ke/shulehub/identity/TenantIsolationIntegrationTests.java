package ke.shulehub.identity;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import ke.shulehub.academics.StreamInfo;
import ke.shulehub.learners.BoardingStatus;
import ke.shulehub.learners.Learners;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.support.IntegrationTest;

class TenantIsolationIntegrationTests extends IntegrationTest {

    @Test
    void oneSchoolCannotReadAnotherSchoolsLearners() throws Exception {
        School schoolA = newSchool();
        School schoolB = newSchool();
        StreamInfo streamA = newStream(schoolA, GradeLevel.GRADE_1, "Grade 1 A");
        Learners.Admitted learnerA = admit(schoolA, streamA, "Imani", "Mutua", BoardingStatus.DAY, null, uniquePhone());
        String directorB = schoolB.director().email();

        // Asking through school B: the record is invisible, exactly like a missing one.
        mvc.perform(getAs(directorB, schoolB.tenantId(), "/api/learners/{id}", learnerA.learnerId()))
                .andExpect(status().isNotFound());
        // Claiming to act in school A: refused, because B's director has no role there.
        mvc.perform(getAs(directorB, schoolA.tenantId(), "/api/learners/{id}", learnerA.learnerId()))
                .andExpect(status().isForbidden());
        // School A's director sees it.
        mvc.perform(getAs(schoolA.director().email(), schoolA.tenantId(), "/api/learners/{id}", learnerA.learnerId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admissionNumber").value(learnerA.admissionNumber()));
    }

    @Test
    void requestsWithoutATenantHeaderAreRejectedForSchoolData() throws Exception {
        School school = newSchool();
        mvc.perform(getAs(school.director().email(), null, "/api/branches"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownLoginsAreToldTheyAreNotLinked() throws Exception {
        mvc.perform(getAs("stranger@example.test", null, "/api/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("not linked")));
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/me"))
                .andExpect(status().isUnauthorized());
    }
}
