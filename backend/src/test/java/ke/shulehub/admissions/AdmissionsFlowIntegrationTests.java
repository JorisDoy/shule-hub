package ke.shulehub.admissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ke.shulehub.academics.StreamInfo;
import ke.shulehub.identity.Grant;
import ke.shulehub.identity.Role;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.support.IntegrationTest;

class AdmissionsFlowIntegrationTests extends IntegrationTest {

    @Test
    void parentEnquiryBecomesAnAdmittedLearnerWithParentAccess() throws Exception {
        School school = newSchool();
        StreamInfo stream = newStream(school, GradeLevel.PP1, "PP1 Rainbow");
        String officer = staff(school, Role.ADMISSIONS_OFFICER, Grant.branch(Role.ADMISSIONS_OFFICER, school.branchId()),
                "Officer");
        String parentEmail = "neema.parent-" + school.slug() + "@example.test";

        String enquiry = json("""
                {'branchCode':'main','gradeLevel':'PP1',
                 'learner':{'givenName':'Neema','familyName':'Wanjiru','dateOfBirth':'2022-03-14','gender':'FEMALE'},
                 'guardian':{'givenName':'Grace','familyName':'Wanjiru','phone':'%s','email':'%s','relationship':'MOTHER'},
                 'notes':'January intake'}
                """.formatted(uniquePhone(), parentEmail));
        mvc.perform(post("/api/public/{slug}/enquiries", school.slug())
                        .contentType(MediaType.APPLICATION_JSON).content(enquiry))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value(org.hamcrest.Matchers.startsWith("APP-")));

        String list = body(mvc.perform(getAs(officer, school.tenantId(), "/api/admissions/applications?branchId={b}",
                        school.branchId()))
                .andExpect(status().isOk()));
        List<String> ids = read(list, "$[*].id");
        assertThat(ids).hasSize(1);
        String id = ids.getFirst();
        assertThat((String) read(list, "$[0].status")).isEqualTo("ENQUIRY");

        for (String target : List.of("APPLIED", "OFFERED", "ACCEPTED")) {
            mvc.perform(postAs(officer, school.tenantId(), json("{'to':'" + target + "','note':'ok'}"),
                            "/api/admissions/applications/{id}/transitions", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(target));
        }

        // A finished workflow step can't be skipped back.
        mvc.perform(postAs(officer, school.tenantId(), json("{'to':'ENQUIRY'}"),
                        "/api/admissions/applications/{id}/transitions", id))
                .andExpect(status().isConflict());

        String admitted = body(mvc.perform(postAs(officer, school.tenantId(), json("{'streamId':'" + stream.id() + "'}"),
                        "/api/admissions/applications/{id}/admit", id))
                .andExpect(status().isOk()));
        assertThat((String) read(admitted, "$.admissionNumber")).isEqualTo("MAIN0001");
        String learnerId = read(admitted, "$.learnerId");

        mvc.perform(getAs(officer, school.tenantId(), "/api/admissions/applications/{id}", id))
                .andExpect(jsonPath("$.status").value("ADMITTED"))
                .andExpect(jsonPath("$.admissionNumber").value("MAIN0001"))
                .andExpect(jsonPath("$.history.length()").value(5));

        mvc.perform(getAs(officer, school.tenantId(), "/api/learners/{id}", learnerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streamName").value("PP1 Rainbow"))
                .andExpect(jsonPath("$.guardians[0].email").value(parentEmail));

        // The guardian can now sign in and sees the school as a parent.
        mvc.perform(getAs(parentEmail, null, "/api/me/memberships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenantId").value(school.tenantId().toString()))
                .andExpect(jsonPath("$[0].parent").value(true));
        mvc.perform(getAs(parentEmail, school.tenantId(), "/api/me/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].admissionNumber").value("MAIN0001"));
    }

    @Test
    void cannotAdmitIntoAFullOrWrongGradeClass() throws Exception {
        School school = newSchool();
        StreamInfo grade4 = newStream(school, GradeLevel.GRADE_4, "Grade 4 East");
        String director = school.director().email();
        String create = json("""
                {'branchId':'%s','gradeLevel':'GRADE_1','boardingRequested':false,
                 'learner':{'givenName':'Baraka','familyName':'Otieno'},
                 'guardian':{'givenName':'Peter','familyName':'Otieno','phone':'%s','relationship':'FATHER'}}
                """.formatted(school.branchId(), uniquePhone()));
        String created = body(mvc.perform(postAs(director, school.tenantId(), create, "/api/admissions/applications"))
                .andExpect(status().isCreated()));
        String id = read(created, "$.id");
        for (String target : List.of("OFFERED", "ACCEPTED")) {
            mvc.perform(postAs(director, school.tenantId(), json("{'to':'" + target + "'}"),
                    "/api/admissions/applications/{id}/transitions", id)).andExpect(status().isOk());
        }
        mvc.perform(postAs(director, school.tenantId(), json("{'streamId':'" + grade4.id() + "'}"),
                        "/api/admissions/applications/{id}/admit", id))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staffWithoutAdmissionsPermissionAreRefused() throws Exception {
        School school = newSchool();
        String bursar = staff(school, Role.BURSAR, Grant.branch(Role.BURSAR, school.branchId()), "Bursar");
        mvc.perform(getAs(bursar, school.tenantId(), "/api/admissions/applications?branchId={b}", school.branchId()))
                .andExpect(status().isForbidden());
    }
}
