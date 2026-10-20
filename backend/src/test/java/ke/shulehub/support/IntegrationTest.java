package ke.shulehub.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import ke.shulehub.academics.AcademicCalendar;
import ke.shulehub.academics.AcademicCalendar.NewTerm;
import ke.shulehub.academics.AcademicYearInfo;
import ke.shulehub.academics.StreamInfo;
import ke.shulehub.academics.Streams;
import ke.shulehub.identity.Actor;
import ke.shulehub.identity.ActorContext;
import ke.shulehub.identity.Grant;
import ke.shulehub.identity.PersonDirectory;
import ke.shulehub.identity.PersonRef;
import ke.shulehub.identity.Role;
import ke.shulehub.identity.RoleAssignments;
import ke.shulehub.learners.BoardingStatus;
import ke.shulehub.learners.Gender;
import ke.shulehub.learners.Learners;
import ke.shulehub.learners.Relationship;
import ke.shulehub.payments.internal.Daraja;
import ke.shulehub.payments.internal.DarajaClient;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.tenancy.TenantContext;
import ke.shulehub.tenancy.TenantProvisioning;
import ke.shulehub.tenancy.TenantProvisioning.NewBranch;
import ke.shulehub.tenancy.TenantProvisioning.NewDirector;
import ke.shulehub.tenancy.TenantProvisioning.Provisioned;

/**
 * Base for integration tests: the full application on a real PostgreSQL (Testcontainers),
 * MockMvc with Spring Security, and Safaricom replaced by a mock. Each test builds its own
 * school with a unique address, so tests don't see each other's data.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected TenantProvisioning provisioning;

    @Autowired
    protected PersonDirectory people;

    @Autowired
    protected RoleAssignments roles;

    @Autowired
    protected AcademicCalendar calendar;

    @Autowired
    protected Streams streams;

    @Autowired
    protected Learners learners;

    @MockitoBean
    protected DarajaClient daraja;

    protected MockMvc mvc;

    @BeforeEach
    void setUpMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        when(daraja.timestamp()).thenReturn("20261020120000");
        when(daraja.stkPush(any(), any())).thenAnswer(invocation -> new Daraja.StkPushResponse("merchant-1",
                "ws_CO_" + UUID.randomUUID(), "0", "Success. Request accepted for processing",
                "Success. Request accepted for processing"));
    }

    /** A provisioned school with one branch, a current academic year (three terms) and its director. */
    protected record School(UUID tenantId, UUID branchId, String slug, String branchCode, PersonRef director,
            AcademicYearInfo year) {

        public UUID currentTermId() {
            LocalDate today = LocalDate.now(ke.shulehub.shared.config.TimeConfig.NAIROBI);
            return year.terms().stream()
                    .filter(term -> !today.isBefore(term.startDate()) && !today.isAfter(term.endDate()))
                    .findFirst().orElseThrow().id();
        }
    }

    protected School newSchool() {
        int n = COUNTER.incrementAndGet();
        String slug = "school-" + n + "-" + UUID.randomUUID().toString().substring(0, 6);
        String directorEmail = "director-" + slug + "@example.test";
        String phone = uniquePhone();
        Provisioned provisioned = provisioning.provision("Test School " + n, slug,
                new NewBranch("Main Campus", "MAIN", "Nairobi", true),
                new NewDirector("Dana", "Director", directorEmail, phone));
        PersonRef director = people.find(provisioned.directorPersonId()).orElseThrow();
        int year = LocalDate.now(ke.shulehub.shared.config.TimeConfig.NAIROBI).getYear();
        AcademicYearInfo academicYear = asDirector(provisioned.tenantId(), director, () -> calendar.createYear(year,
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31),
                List.of(new NewTerm(1, LocalDate.of(year, 1, 1), LocalDate.of(year, 4, 30)),
                        new NewTerm(2, LocalDate.of(year, 5, 1), LocalDate.of(year, 8, 31)),
                        new NewTerm(3, LocalDate.of(year, 9, 1), LocalDate.of(year, 12, 31)))));
        return new School(provisioned.tenantId(), provisioned.branchId(), slug, "MAIN", director, academicYear);
    }

    /** A random, valid Kenyan mobile number so people created by different tests never collide. */
    protected static String uniquePhone() {
        return String.format(Locale.ROOT, "07%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    protected StreamInfo newStream(School school, GradeLevel grade, String name) {
        return asDirector(school, () -> streams.create(school.branchId(), school.year().id(), grade, name, 40));
    }

    /** Admits a learner directly; returns the learner id. */
    protected Learners.Admitted admit(School school, StreamInfo stream, String given, String family,
            BoardingStatus boarding, String guardianEmail, String guardianPhone) {
        return asDirector(school, () -> learners.admit(new Learners.AdmitCommand(school.branchId(), stream.id(), given,
                family, null, LocalDate.now().minusYears(10), Gender.FEMALE, stream.gradeLevel(), boarding,
                new Learners.GuardianDetails("Grace", family, guardianPhone, guardianEmail, Relationship.MOTHER))));
    }

    /** Creates a staff member with a role and returns their email (the login used in requests). */
    protected String staff(School school, Role role, Grant grant, String name) {
        String email = name.toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID().toString().substring(0, 6) + "@example.test";
        return asDirector(school, () -> {
            PersonRef person = people.findOrCreate(name, "Staff", null, email);
            roles.assign(person.id(), grant);
            return email;
        });
    }

    protected <T> T asDirector(School school, java.util.function.Supplier<T> action) {
        return asDirector(school.tenantId(), school.director(), action);
    }

    private static <T> T asDirector(UUID tenantId, PersonRef director, java.util.function.Supplier<T> action) {
        Actor actor = new Actor(director, List.of(Grant.group(Role.GROUP_DIRECTOR)), false);
        return TenantContext.callAs(tenantId, () -> ActorContext.callAs(actor, action));
    }

    // ---- HTTP helpers: requests signed as the person with this (verified) email ----

    protected MockHttpServletRequestBuilder getAs(String email, UUID tenantId, String url, Object... vars) {
        return sign(get(url, vars), email, tenantId);
    }

    protected MockHttpServletRequestBuilder postAs(String email, UUID tenantId, String body, String url, Object... vars) {
        return sign(post(url, vars).contentType(MediaType.APPLICATION_JSON).content(body), email, tenantId);
    }

    protected MockHttpServletRequestBuilder putAs(String email, UUID tenantId, String body, String url, Object... vars) {
        return sign(put(url, vars).contentType(MediaType.APPLICATION_JSON).content(body), email, tenantId);
    }

    protected MockHttpServletRequestBuilder patchAs(String email, UUID tenantId, String body, String url, Object... vars) {
        return sign(patch(url, vars).contentType(MediaType.APPLICATION_JSON).content(body), email, tenantId);
    }

    private static MockHttpServletRequestBuilder sign(MockHttpServletRequestBuilder request, String email,
            UUID tenantId) {
        request.with(jwt().jwt(token -> token.subject("kc-" + email).claim("email", email).claim("email_verified", true)));
        if (tenantId != null) {
            request.header("X-Tenant-Id", tenantId.toString());
        }
        return request;
    }

    protected static String json(String body) {
        return body.replace('\'', '"');
    }

    protected static <T> T read(String json, String path) {
        return JsonPath.read(json, path);
    }

    protected String body(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }
}
