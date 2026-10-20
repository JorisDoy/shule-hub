package ke.shulehub.demo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ke.shulehub.academics.AcademicCalendar;
import ke.shulehub.academics.AcademicCalendar.NewTerm;
import ke.shulehub.academics.AcademicYearInfo;
import ke.shulehub.academics.LearningAreaInfo;
import ke.shulehub.academics.LearningAreas;
import ke.shulehub.academics.StreamInfo;
import ke.shulehub.academics.Streams;
import ke.shulehub.academics.TermInfo;
import ke.shulehub.assessment.AssessmentType;
import ke.shulehub.assessment.Assessments;
import ke.shulehub.fees.FeeAppliesTo;
import ke.shulehub.fees.FeeSetup;
import ke.shulehub.fees.FeeViews.FeeStructureView;
import ke.shulehub.fees.FeeViews.VoteHeadView;
import ke.shulehub.fees.Invoicing;
import ke.shulehub.fees.Ledger;
import ke.shulehub.fees.PaymentChannel;
import ke.shulehub.identity.Actor;
import ke.shulehub.identity.ActorContext;
import ke.shulehub.identity.Grant;
import ke.shulehub.identity.PersonDirectory;
import ke.shulehub.identity.PersonRef;
import ke.shulehub.identity.Role;
import ke.shulehub.identity.RoleAssignments;
import ke.shulehub.learners.BoardingStatus;
import ke.shulehub.learners.Gender;
import ke.shulehub.learners.LearnerDirectory;
import ke.shulehub.learners.Learners;
import ke.shulehub.learners.Learners.GuardianDetails;
import ke.shulehub.learners.Relationship;
import ke.shulehub.payments.MpesaChannels;
import ke.shulehub.payments.MpesaChannelType;
import ke.shulehub.payments.MpesaEnvironment;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.school.LevelBand;
import ke.shulehub.shared.tenancy.TenantContext;
import ke.shulehub.tenancy.TenantDirectory;
import ke.shulehub.tenancy.TenantProvisioning;
import ke.shulehub.tenancy.TenantProvisioning.NewBranch;
import ke.shulehub.tenancy.TenantProvisioning.NewDirector;
import ke.shulehub.tenancy.TenantProvisioning.Provisioned;

/**
 * Seeds "Shule Hub Demo Academy" (address {@code demo}) for local trials and the public sandbox:
 * staff and a parent whose emails match the Keycloak demo users, three classes, learners, fees,
 * invoices, a payment and marks. Runs only with {@code shulehub.demo.enabled=true}, once.
 */
@Component
@ConditionalOnProperty(name = "shulehub.demo.enabled", havingValue = "true")
class DemoSchoolSeeder implements ApplicationRunner {

    static final String SLUG = "demo";
    private static final String SANDBOX_SHORTCODE = "174379";
    private static final String SANDBOX_PASSKEY = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919";
    private static final Logger log = LoggerFactory.getLogger(DemoSchoolSeeder.class);

    private final TenantDirectory tenants;
    private final TenantProvisioning provisioning;
    private final PersonDirectory people;
    private final RoleAssignments roles;
    private final AcademicCalendar calendar;
    private final Streams streams;
    private final LearningAreas learningAreas;
    private final Learners learners;
    private final LearnerDirectory learnerDirectory;
    private final FeeSetup feeSetup;
    private final Invoicing invoicing;
    private final Ledger ledger;
    private final Assessments assessments;
    private final MpesaChannels mpesaChannels;
    private final Environment environment;
    private final Clock clock;

    DemoSchoolSeeder(TenantDirectory tenants, TenantProvisioning provisioning, PersonDirectory people,
            RoleAssignments roles, AcademicCalendar calendar, Streams streams, LearningAreas learningAreas,
            Learners learners, LearnerDirectory learnerDirectory, FeeSetup feeSetup, Invoicing invoicing, Ledger ledger,
            Assessments assessments, MpesaChannels mpesaChannels, Environment environment, Clock clock) {
        this.tenants = tenants;
        this.provisioning = provisioning;
        this.people = people;
        this.roles = roles;
        this.calendar = calendar;
        this.streams = streams;
        this.learningAreas = learningAreas;
        this.learners = learners;
        this.learnerDirectory = learnerDirectory;
        this.feeSetup = feeSetup;
        this.invoicing = invoicing;
        this.ledger = ledger;
        this.assessments = assessments;
        this.mpesaChannels = mpesaChannels;
        this.environment = environment;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (tenants.findBySlug(SLUG).isPresent()) {
            log.info("Demo school already exists; skipping seed");
            return;
        }
        Provisioned demo = provisioning.provision("Shule Hub Demo Academy", SLUG,
                new NewBranch("Ruaka Campus", "RUA", "Kiambu", true),
                new NewDirector("Wanjiku", "Kamau", "director@demo.shulehub.local", "0700000001"));
        PersonRef director = people.find(demo.directorPersonId()).orElseThrow();
        Actor actor = new Actor(director, List.of(Grant.group(Role.GROUP_DIRECTOR)), false);
        TenantContext.runAs(demo.tenantId(), () -> ActorContext.callAs(actor, () -> {
            seed(demo.branchId());
            return null;
        }));
        log.info("Seeded demo school (tenant {})", demo.tenantId());
    }

    private void seed(UUID branchId) {
        PersonRef bursar = people.findOrCreate("Otieno", "Ouma", "0700000002", "bursar@demo.shulehub.local");
        roles.assign(bursar.id(), Grant.branch(Role.BURSAR, branchId));
        PersonRef admissions = people.findOrCreate("Chebet", "Kiprop", "0700000003", "admissions@demo.shulehub.local");
        roles.assign(admissions.id(), Grant.branch(Role.ADMISSIONS_OFFICER, branchId));
        PersonRef deputy = people.findOrCreate("Mwangi", "Njoroge", "0700000005", "deputy@demo.shulehub.local");
        roles.assign(deputy.id(), Grant.branch(Role.DEPUTY, branchId));

        int year = LocalDate.now(clock).getYear();
        AcademicYearInfo academicYear = calendar.createYear(year, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31),
                List.of(new NewTerm(1, LocalDate.of(year, 1, 5), LocalDate.of(year, 4, 3)),
                        new NewTerm(2, LocalDate.of(year, 4, 27), LocalDate.of(year, 7, 31)),
                        new NewTerm(3, LocalDate.of(year, 8, 24), LocalDate.of(year, 11, 20))));
        TermInfo term = calendar.currentTerm().orElseThrow();

        StreamInfo pp1 = streams.create(branchId, academicYear.id(), GradeLevel.PP1, "PP1 Rainbow", 25);
        StreamInfo grade4 = streams.create(branchId, academicYear.id(), GradeLevel.GRADE_4, "Grade 4 East", 40);
        StreamInfo grade7 = streams.create(branchId, academicYear.id(), GradeLevel.GRADE_7, "Grade 7 Blue", 40);

        PersonRef teacher = people.findOrCreate("Achieng", "Atieno", "0700000004", "teacher@demo.shulehub.local");
        roles.assign(teacher.id(), Grant.stream(Role.CLASS_TEACHER, branchId, grade7.id()));

        GuardianDetails demoParent = new GuardianDetails("Grace", "Wanjiru", "0711000001", "parent@demo.shulehub.local",
                Relationship.MOTHER);
        UUID amani = admit(branchId, grade7, "Amani", "Wanjiru", Gender.MALE, BoardingStatus.BOARDING, demoParent);
        admit(branchId, grade4, "Neema", "Wanjiru", Gender.FEMALE, BoardingStatus.DAY, demoParent);
        UUID baraka = admit(branchId, grade7, "Baraka", "Otieno", Gender.MALE, BoardingStatus.DAY,
                new GuardianDetails("Peter", "Otieno", "0722000002", null, Relationship.FATHER));
        UUID imani = admit(branchId, grade7, "Imani", "Mutua", Gender.FEMALE, BoardingStatus.DAY,
                new GuardianDetails("Ruth", "Mutua", "0733000003", null, Relationship.MOTHER));
        admit(branchId, grade4, "Zawadi", "Kariuki", Gender.FEMALE, BoardingStatus.DAY,
                new GuardianDetails("James", "Kariuki", "0744000004", null, Relationship.FATHER));
        admit(branchId, pp1, "Jabali", "Mohamed", Gender.MALE, BoardingStatus.DAY,
                new GuardianDetails("Amina", "Mohamed", "0755000005", null, Relationship.MOTHER));

        List<VoteHeadView> heads = feeSetup.voteHeads();
        UUID tuition = head(heads, "TUITION");
        UUID boarding = head(heads, "BOARDING");
        UUID lunch = head(heads, "LUNCH");
        UUID activity = head(heads, "ACTIVITY");
        structure(branchId, term, GradeLevel.PP1, List.of(
                new FeeSetup.ItemInput(tuition, new BigDecimal("18000"), FeeAppliesTo.ALL),
                new FeeSetup.ItemInput(lunch, new BigDecimal("4500"), FeeAppliesTo.DAY)));
        structure(branchId, term, GradeLevel.GRADE_4, List.of(
                new FeeSetup.ItemInput(tuition, new BigDecimal("24000"), FeeAppliesTo.ALL),
                new FeeSetup.ItemInput(lunch, new BigDecimal("5000"), FeeAppliesTo.DAY),
                new FeeSetup.ItemInput(activity, new BigDecimal("1500"), FeeAppliesTo.ALL)));
        structure(branchId, term, GradeLevel.GRADE_7, List.of(
                new FeeSetup.ItemInput(tuition, new BigDecimal("32000"), FeeAppliesTo.ALL),
                new FeeSetup.ItemInput(boarding, new BigDecimal("28000"), FeeAppliesTo.BOARDING),
                new FeeSetup.ItemInput(lunch, new BigDecimal("5500"), FeeAppliesTo.DAY),
                new FeeSetup.ItemInput(activity, new BigDecimal("2000"), FeeAppliesTo.ALL)));
        invoicing.generate(branchId, term.id(), term.startDate().plusDays(21));
        ledger.recordManual(baraka, new BigDecimal("20000"), PaymentChannel.CASH, "RCPT-0001", term.startDate().plusDays(3));
        ledger.recordManual(amani, new BigDecimal("60000"), PaymentChannel.BANK, "EQ-552190", term.startDate().plusDays(10));

        LearningAreaInfo maths = learningAreas.list(LevelBand.JUNIOR_SCHOOL).stream()
                .filter(area -> area.code().equals("MATH")).findFirst().orElseThrow();
        Assessments.AssessmentView cat = assessments.create(new Assessments.NewAssessment("Mathematics CAT 1",
                AssessmentType.CAT, term.id(), grade7.id(), maths.id(), new BigDecimal("50"), BigDecimal.ONE,
                term.startDate().plusDays(30)));
        assessments.saveScores(cat.id(), List.of(
                new Assessments.ScoreInput(amani, new BigDecimal("41"), null, "Strong on algebra"),
                new Assessments.ScoreInput(baraka, new BigDecimal("29"), null, null),
                new Assessments.ScoreInput(imani, new BigDecimal("37.5"), null, "Show your working")));

        String consumerKey = environment.getProperty("SHULEHUB_MPESA_SANDBOX_CONSUMER_KEY");
        String consumerSecret = environment.getProperty("SHULEHUB_MPESA_SANDBOX_CONSUMER_SECRET");
        if (consumerKey != null && !consumerKey.isBlank() && consumerSecret != null && !consumerSecret.isBlank()) {
            mpesaChannels.create(new MpesaChannels.NewChannel(branchId, SANDBOX_SHORTCODE, null,
                    MpesaChannelType.PAYBILL, MpesaEnvironment.SANDBOX, consumerKey, consumerSecret,
                    environment.getProperty("SHULEHUB_MPESA_SANDBOX_PASSKEY", SANDBOX_PASSKEY)));
            log.info("Demo M-Pesa sandbox channel created for shortcode {}", SANDBOX_SHORTCODE);
        }
        log.info("Demo learners: {}", learnerDirectory.activeInBranch(branchId).size());
    }

    private UUID admit(UUID branchId, StreamInfo stream, String given, String family, Gender gender,
            BoardingStatus boarding, GuardianDetails guardian) {
        return learners.admit(new Learners.AdmitCommand(branchId, stream.id(), given, family, null, null, gender,
                stream.gradeLevel(), boarding, guardian)).learnerId();
    }

    private void structure(UUID branchId, TermInfo term, GradeLevel grade, List<FeeSetup.ItemInput> items) {
        FeeStructureView created = feeSetup.create(branchId, term.id(), grade, grade.label() + " " + term.label(), items);
        feeSetup.publish(created.id());
    }

    private static UUID head(List<VoteHeadView> heads, String code) {
        return heads.stream().filter(head -> head.code().equals(code)).findFirst().orElseThrow().id();
    }
}
