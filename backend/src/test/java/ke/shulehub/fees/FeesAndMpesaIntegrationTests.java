package ke.shulehub.fees;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ke.shulehub.academics.StreamInfo;
import ke.shulehub.identity.Grant;
import ke.shulehub.identity.Role;
import ke.shulehub.learners.BoardingStatus;
import ke.shulehub.learners.Learners;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.support.IntegrationTest;

class FeesAndMpesaIntegrationTests extends IntegrationTest {

    private record Setup(School school, String bursar, Learners.Admitted dayLearner, Learners.Admitted boarder,
            String termId, String callbackToken) {
    }

    private Setup setUpSchoolWithInvoices() throws Exception {
        School school = newSchool();
        StreamInfo grade7 = newStream(school, GradeLevel.GRADE_7, "Grade 7 Blue");
        Learners.Admitted day = admit(school, grade7, "Baraka", "Otieno", BoardingStatus.DAY, null, uniquePhone());
        Learners.Admitted boarder = admit(school, grade7, "Amani", "Wanjiru", BoardingStatus.BOARDING, null,
                uniquePhone());
        String bursar = staff(school, Role.BURSAR, Grant.branch(Role.BURSAR, school.branchId()), "Bursar");
        String termId = school.currentTermId().toString();

        String heads = body(mvc.perform(getAs(bursar, school.tenantId(), "/api/fees/vote-heads")).andExpect(status().isOk()));
        String tuition = voteHead(heads, "TUITION");
        String boarding = voteHead(heads, "BOARDING");
        String lunch = voteHead(heads, "LUNCH");

        String structure = body(mvc.perform(postAs(bursar, school.tenantId(), json("""
                {'branchId':'%s','termId':'%s','gradeLevel':'GRADE_7','name':'Grade 7 fees',
                 'items':[{'voteHeadId':'%s','amount':30000,'appliesTo':'ALL'},
                          {'voteHeadId':'%s','amount':25000,'appliesTo':'BOARDING'},
                          {'voteHeadId':'%s','amount':5000,'appliesTo':'DAY'}]}
                """.formatted(school.branchId(), termId, tuition, boarding, lunch)), "/api/fees/structures"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayTotal").value(35000.0))
                .andExpect(jsonPath("$.boardingTotal").value(55000.0)));
        mvc.perform(postAs(bursar, school.tenantId(), "{}", "/api/fees/structures/{id}/publish",
                        (String) read(structure, "$.id")))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        String dueOn = LocalDate.now().minusDays(40).toString();
        mvc.perform(postAs(bursar, school.tenantId(), json("{'branchId':'%s','termId':'%s','dueOn':'%s'}"
                        .formatted(school.branchId(), termId, dueOn)), "/api/fees/invoices/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2));
        // Running it again is harmless.
        mvc.perform(postAs(bursar, school.tenantId(), json("{'branchId':'%s','termId':'%s','dueOn':'%s'}"
                        .formatted(school.branchId(), termId, dueOn)), "/api/fees/invoices/generate"))
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.skipped").value(2));

        String channel = body(mvc.perform(postAs(bursar, school.tenantId(), json("""
                {'branchId':'%s','shortcode':'174379','channelType':'PAYBILL','environment':'SANDBOX',
                 'consumerKey':'key','consumerSecret':'secret','passkey':'passkey'}
                """.formatted(school.branchId())), "/api/payments/mpesa/channels"))
                .andExpect(status().isCreated()));
        String stkUrl = read(channel, "$.callbackUrls.stk");
        assertThat(stkUrl).startsWith("https://callbacks.test.shulehub.example/api/callbacks/mobile-money/");
        String token = stkUrl.replaceAll(".*/mobile-money/([^/]+)/stk$", "$1");
        return new Setup(school, bursar, day, boarder, termId, token);
    }

    @Test
    void stkPushPaymentIsRecordedOnceAndAppliedToTheInvoice() throws Exception {
        Setup s = setUpSchoolWithInvoices();
        String learnerId = s.dayLearner().learnerId().toString();

        mvc.perform(getAs(s.bursar(), s.school().tenantId(), "/api/fees/learners/{id}/statement", learnerId))
                .andExpect(jsonPath("$.balance").value(35000.0));

        String stk = body(mvc.perform(postAs(s.bursar(), s.school().tenantId(), json(
                        "{'learnerId':'%s','amount':20000,'phone':'0712345678'}".formatted(learnerId)), "/api/fees/stk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED")));
        String intentId = read(stk, "$.intentId");
        String status = body(mvc.perform(getAs(s.bursar(), s.school().tenantId(), "/api/fees/stk/{id}", intentId)));
        assertThat((String) read(status, "$.status")).isEqualTo("SUBMITTED");
        String checkoutRequestId = jdbcCheckoutId(intentId);

        String callback = """
                {"Body":{"stkCallback":{"MerchantRequestID":"merchant-1","CheckoutRequestID":"%s","ResultCode":0,
                "ResultDesc":"The service request is processed successfully.","CallbackMetadata":{"Item":[
                {"Name":"Amount","Value":20000.00},{"Name":"MpesaReceiptNumber","Value":"TJK1ABC2DE"},
                {"Name":"TransactionDate","Value":20261020121530},{"Name":"PhoneNumber","Value":254712345678}]}}}}
                """.formatted(checkoutRequestId);
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/callbacks/mobile-money/{token}/stk", s.callbackToken())
                            .contentType(MediaType.APPLICATION_JSON).content(callback))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ResultCode").value(0));
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                mvc.perform(getAs(s.bursar(), s.school().tenantId(), "/api/fees/learners/{id}/statement", learnerId))
                        .andExpect(jsonPath("$.totalPaid").value(20000.0))
                        .andExpect(jsonPath("$.balance").value(15000.0)));

        mvc.perform(getAs(s.bursar(), s.school().tenantId(), "/api/fees/stk/{id}", intentId))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.mpesaReceipt").value("TJK1ABC2DE"));
        mvc.perform(getAs(s.bursar(), s.school().tenantId(), "/api/fees/invoices?branchId={b}&learnerId={l}",
                        s.school().branchId(), learnerId))
                .andExpect(jsonPath("$[0].status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$[0].paid").value(20000.0));
        // Tuition (priority 10) is paid before lunch (priority 30).
        String invoices = body(mvc.perform(getAs(s.bursar(), s.school().tenantId(),
                "/api/fees/invoices?branchId={b}&learnerId={l}", s.school().branchId(), learnerId)));
        String invoice = body(mvc.perform(getAs(s.bursar(), s.school().tenantId(), "/api/fees/invoices/{id}",
                (String) read(invoices, "$[0].id"))));
        List<Map<String, Object>> lines = read(invoice, "$.lines");
        assertThat(lines).extracting(line -> line.get("voteHeadName")).containsExactly("Tuition", "Lunch");
        assertThat(((Number) lines.get(0).get("paid")).doubleValue()).isEqualTo(20000.0);
        assertThat(((Number) lines.get(1).get("paid")).doubleValue()).isEqualTo(0.0);
    }

    @Test
    void paybillPaymentWithUnknownAccountGoesToSuspenseUntilAssigned() throws Exception {
        Setup s = setUpSchoolWithInvoices();
        String validation = """
                {"TransactionType":"Pay Bill","TransID":"TJK9ZZZ111","TransTime":"20261020101010","TransAmount":"1000.00",
                 "BusinessShortCode":"174379","BillRefNumber":"NOPE123","MSISDN":"254700000000","FirstName":"JOHN"}
                """;
        mvc.perform(post("/api/callbacks/mobile-money/{token}/c2b/validation", s.callbackToken())
                        .contentType(MediaType.APPLICATION_JSON).content(validation))
                .andExpect(jsonPath("$.ResultCode").value("C2B00012"));

        mvc.perform(post("/api/callbacks/mobile-money/{token}/c2b/confirmation", s.callbackToken())
                        .contentType(MediaType.APPLICATION_JSON).content(validation))
                .andExpect(jsonPath("$.ResultCode").value("0"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                mvc.perform(getAs(s.bursar(), s.school().tenantId(), "/api/fees/payments?branchId={b}&status=SUSPENSE",
                                s.school().branchId()))
                        .andExpect(jsonPath("$[0].reference").value("TJK9ZZZ111"))
                        .andExpect(jsonPath("$[0].learnerId").doesNotExist()));
        String suspense = body(mvc.perform(getAs(s.bursar(), s.school().tenantId(),
                "/api/fees/payments?branchId={b}&status=SUSPENSE", s.school().branchId())));
        String paymentId = read(suspense, "$[0].id");

        String boarderId = s.boarder().learnerId().toString();
        mvc.perform(postAs(s.bursar(), s.school().tenantId(), json("{'learnerId':'" + boarderId + "'}"),
                        "/api/fees/payments/{id}/assign", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALLOCATED"))
                .andExpect(jsonPath("$.allocated").value(1000.0));
        mvc.perform(getAs(s.bursar(), s.school().tenantId(), "/api/fees/learners/{id}/statement", boarderId))
                .andExpect(jsonPath("$.balance").value(54000.0));
    }

    @Test
    void paybillPaymentUsingTheAdmissionNumberIsMatchedAutomatically() throws Exception {
        Setup s = setUpSchoolWithInvoices();
        String admissionNumber = s.dayLearner().admissionNumber();
        String payload = """
                {"TransactionType":"Pay Bill","TransID":"TJK7AAA222","TransTime":"20261020101010","TransAmount":"40000.00",
                 "BusinessShortCode":"174379","BillRefNumber":"%s","MSISDN":"254711111111","FirstName":"PETER"}
                """.formatted(admissionNumber.toLowerCase());
        mvc.perform(post("/api/callbacks/mobile-money/{token}/c2b/validation", s.callbackToken())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(jsonPath("$.ResultCode").value("0"));
        mvc.perform(post("/api/callbacks/mobile-money/{token}/c2b/confirmation", s.callbackToken())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());

        String learnerId = s.dayLearner().learnerId().toString();
        // 40,000 against a 35,000 invoice: paid in full with 5,000 credit.
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                mvc.perform(getAs(s.bursar(), s.school().tenantId(), "/api/fees/learners/{id}/statement", learnerId))
                        .andExpect(jsonPath("$.balance").value(-5000.0)));
        mvc.perform(getAs(s.bursar(), s.school().tenantId(), "/api/fees/invoices?branchId={b}&learnerId={l}",
                        s.school().branchId(), learnerId))
                .andExpect(jsonPath("$[0].status").value("PAID"));
    }

    @Test
    void arrearsAreAgedFromTheDueDateAndCashIsRecorded() throws Exception {
        Setup s = setUpSchoolWithInvoices();
        mvc.perform(postAs(s.bursar(), s.school().tenantId(), json("""
                {'learnerId':'%s','amount':5000,'channel':'CASH','reference':'R-001'}
                """.formatted(s.dayLearner().learnerId())), "/api/fees/payments"))
                .andExpect(status().isCreated());
        // The same receipt again is not double-counted.
        mvc.perform(postAs(s.bursar(), s.school().tenantId(), json("""
                {'learnerId':'%s','amount':5000,'channel':'CASH','reference':'r-001'}
                """.formatted(s.dayLearner().learnerId())), "/api/fees/payments"))
                .andExpect(status().isCreated());

        mvc.perform(getAs(s.bursar(), s.school().tenantId(), "/api/fees/arrears?branchId={b}", s.school().branchId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.days31To60").value(85000.0))
                .andExpect(jsonPath("$.totals.total").value(85000.0))
                .andExpect(jsonPath("$.learners[0].total").value(55000.0));
    }

    @Test
    void parentsCanOnlyPayForTheirOwnChildren() throws Exception {
        Setup s = setUpSchoolWithInvoices();
        String parentEmail = "parent-" + s.school().slug() + "@example.test";
        StreamInfo grade7 = asDirector(s.school(), () -> streams.list(s.school().branchId(), null).getFirst());
        Learners.Admitted mine = admit(s.school(), grade7, "Zawadi", "Kariuki", BoardingStatus.DAY, parentEmail,
                uniquePhone());

        mvc.perform(postAs(parentEmail, s.school().tenantId(), json("{'amount':1000,'phone':'0712345678'}"),
                        "/api/me/children/{id}/pay", mine.learnerId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        mvc.perform(postAs(parentEmail, s.school().tenantId(), json("{'amount':1000,'phone':'0712345678'}"),
                        "/api/me/children/{id}/pay", s.dayLearner().learnerId()))
                .andExpect(status().isNotFound());
        mvc.perform(getAs(parentEmail, s.school().tenantId(), "/api/fees/learners/{id}/statement",
                        mine.learnerId()))
                .andExpect(status().isForbidden());
    }

    private String jdbcCheckoutId(String intentId) {
        return context.getBean(org.springframework.jdbc.core.JdbcTemplate.class).queryForObject(
                "select checkout_request_id from payment_intent where id = ?::uuid", String.class, intentId);
    }

    private static String voteHead(String heads, String code) {
        List<String> ids = read(heads, "$[?(@.code == '" + code + "')].id");
        return ids.getFirst();
    }
}
