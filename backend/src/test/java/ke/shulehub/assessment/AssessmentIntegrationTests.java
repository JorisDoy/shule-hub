package ke.shulehub.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;

import ke.shulehub.academics.StreamInfo;
import ke.shulehub.identity.Grant;
import ke.shulehub.identity.Role;
import ke.shulehub.learners.BoardingStatus;
import ke.shulehub.learners.Learners;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.support.IntegrationTest;

class AssessmentIntegrationTests extends IntegrationTest {

    @Test
    void junior_school_marks_are_banded_on_the_knec_scale_and_released_to_parents() throws Exception {
        School school = newSchool();
        StreamInfo grade7 = newStream(school, GradeLevel.GRADE_7, "Grade 7 Blue");
        String parentEmail = "parent-" + school.slug() + "@example.test";
        Learners.Admitted amani = admit(school, grade7, "Amani", "Wanjiru", BoardingStatus.DAY, parentEmail,
                uniquePhone());
        Learners.Admitted baraka = admit(school, grade7, "Baraka", "Otieno", BoardingStatus.DAY, null, uniquePhone());
        String teacher = staff(school, Role.CLASS_TEACHER,
                Grant.stream(Role.CLASS_TEACHER, school.branchId(), grade7.id()), "Teacher");
        String deputy = staff(school, Role.DEPUTY, Grant.branch(Role.DEPUTY, school.branchId()), "Deputy");
        String termId = school.currentTermId().toString();

        String areas = body(mvc.perform(getAs(teacher, school.tenantId(), "/api/learning-areas?levelBand=JUNIOR_SCHOOL"))
                .andExpect(status().isOk()));
        List<String> mathIds = read(areas, "$[?(@.code == 'MATH')].id");
        assertThat((List<?>) read(areas, "$")).hasSize(9);
        String maths = mathIds.getFirst();

        String created = body(mvc.perform(postAs(teacher, school.tenantId(), json("""
                {'title':'Mathematics CAT 1','type':'CAT','termId':'%s','streamId':'%s','learningAreaId':'%s',
                 'maxScore':50,'weight':1,'assessedOn':'%s'}
                """.formatted(termId, grade7.id(), maths, java.time.LocalDate.now())), "/api/assessment/assessments"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.learnerCount").value(2)));
        String assessmentId = read(created, "$.id");

        // 37.25 / 50 = 74.5% → rounds to 75% → EE2 (7 points); 45 / 50 = 90% → EE1.
        mvc.perform(putAs(teacher, school.tenantId(), json("""
                {'scores':[{'learnerId':'%s','rawScore':37.25,'comment':'Good work'},
                           {'learnerId':'%s','rawScore':45}]}
                """.formatted(amani.learnerId(), baraka.learnerId())), "/api/assessment/assessments/{id}/scores",
                        assessmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[?(@.learnerName == 'Amani Wanjiru')].percent").value(75))
                .andExpect(jsonPath("$.rows[?(@.learnerName == 'Amani Wanjiru')].bandCode").value("EE2"))
                .andExpect(jsonPath("$.rows[?(@.learnerName == 'Baraka Otieno')].bandCode").value("EE1"))
                .andExpect(jsonPath("$.assessment.scoredCount").value(2));

        // Marks above the maximum are refused.
        mvc.perform(putAs(teacher, school.tenantId(), json("{'scores':[{'learnerId':'%s','rawScore':51}]}"
                        .formatted(amani.learnerId())), "/api/assessment/assessments/{id}/scores", assessmentId))
                .andExpect(status().isBadRequest());

        mvc.perform(getAs(teacher, school.tenantId(), "/api/assessment/report-cards/{l}/{t}", amani.learnerId(), termId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scale.code").value("KNEC_8_LEVEL"))
                .andExpect(jsonPath("$.areas.length()").value(9))
                .andExpect(jsonPath("$.areas[?(@.name == 'Mathematics')].points").value(7))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mvc.perform(putAs(teacher, school.tenantId(), json("""
                {'classTeacherComment':'A focused term.','areaComments':[{'learningAreaId':'%s','comment':'Strong algebra'}]}
                """.formatted(maths)), "/api/assessment/report-cards/{l}/{t}", amani.learnerId(), termId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classTeacherComment").value("A focused term."));

        // Not visible to the parent until released.
        mvc.perform(getAs(parentEmail, school.tenantId(), "/api/me/children/{l}/report-cards/{t}", amani.learnerId(),
                        termId))
                .andExpect(status().isNotFound());

        // A class teacher can't approve; the deputy can, then releases the class.
        mvc.perform(postAs(teacher, school.tenantId(), "{}", "/api/assessment/report-cards/{l}/{t}/approve",
                        amani.learnerId(), termId))
                .andExpect(status().isForbidden());
        mvc.perform(postAs(deputy, school.tenantId(), "{}", "/api/assessment/report-cards/{l}/{t}/approve",
                        amani.learnerId(), termId))
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(postAs(deputy, school.tenantId(), json("{'streamId':'%s','termId':'%s'}".formatted(grade7.id(), termId)),
                        "/api/assessment/report-cards/release"))
                .andExpect(jsonPath("$.released").value(1));

        mvc.perform(getAs(parentEmail, school.tenantId(), "/api/me/children/{l}/report-cards", amani.learnerId()))
                .andExpect(jsonPath("$[0].termId").value(termId));
        mvc.perform(getAs(parentEmail, school.tenantId(), "/api/me/children/{l}/report-cards/{t}", amani.learnerId(),
                        termId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areas[?(@.name == 'Mathematics')].bandCode").value("EE2"))
                .andExpect(jsonPath("$.areas[?(@.name == 'Mathematics')].comment").value("Strong algebra"));

        // Released cards are frozen.
        mvc.perform(putAs(teacher, school.tenantId(), json("{'classTeacherComment':'changed'}"),
                        "/api/assessment/report-cards/{l}/{t}", amani.learnerId(), termId))
                .andExpect(status().isConflict());
    }

    @Test
    void rubric_assessments_use_the_four_level_scale_below_junior_school() throws Exception {
        School school = newSchool();
        StreamInfo grade4 = newStream(school, GradeLevel.GRADE_4, "Grade 4 East");
        Learners.Admitted zawadi = admit(school, grade4, "Zawadi", "Kariuki", BoardingStatus.DAY, null, uniquePhone());
        String director = school.director().email();
        String termId = school.currentTermId().toString();
        String areas = body(mvc.perform(getAs(director, school.tenantId(), "/api/learning-areas?levelBand=UPPER_PRIMARY")));
        String english = ((List<String>) read(areas, "$[?(@.code == 'ENG')].id")).getFirst();

        String created = body(mvc.perform(postAs(director, school.tenantId(), json("""
                {'title':'Reading observation','type':'OBSERVATION','termId':'%s','streamId':'%s','learningAreaId':'%s',
                 'assessedOn':'%s'}
                """.formatted(termId, grade4.id(), english, java.time.LocalDate.now())), "/api/assessment/assessments"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maxScore").doesNotExist()));
        String id = read(created, "$.id");
        mvc.perform(putAs(director, school.tenantId(), json("{'scores':[{'learnerId':'%s','rubricLevel':'me'}]}"
                        .formatted(zawadi.learnerId())), "/api/assessment/assessments/{id}/scores", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scale.code").value("CBE_4_LEVEL"))
                .andExpect(jsonPath("$.rows[0].bandCode").value("ME"));
        mvc.perform(getAs(director, school.tenantId(), "/api/assessment/report-cards/{l}/{t}", zawadi.learnerId(), termId))
                .andExpect(jsonPath("$.areas[?(@.name == 'English')].bandCode").value("ME"))
                .andExpect(jsonPath("$.areas[?(@.name == 'English')].points").value(3));
    }
}
