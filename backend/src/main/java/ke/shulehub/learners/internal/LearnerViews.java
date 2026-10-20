package ke.shulehub.learners.internal;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ke.shulehub.academics.StreamInfo;
import ke.shulehub.academics.Streams;
import ke.shulehub.identity.PersonDirectory;
import ke.shulehub.identity.PersonRef;
import ke.shulehub.learners.LearnerDetail;
import ke.shulehub.learners.LearnerSummary;

/** Builds learner views in batches: one query each for enrolments, streams and contacts. */
@Component
public class LearnerViews {

    private final EnrolmentRepository enrolments;
    private final GuardianLinkRepository guardians;
    private final Streams streams;
    private final PersonDirectory people;

    public LearnerViews(EnrolmentRepository enrolments, GuardianLinkRepository guardians, Streams streams,
            PersonDirectory people) {
        this.enrolments = enrolments;
        this.guardians = guardians;
        this.streams = streams;
        this.people = people;
    }

    public List<LearnerSummary> summaries(Collection<Learner> learners) {
        if (learners.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = learners.stream().map(Learner::getId).toList();
        Map<UUID, Enrolment> currentEnrolment = enrolments.findByLearnerIdInAndStatus(ids, Enrolment.ACTIVE).stream()
                .collect(Collectors.toMap(Enrolment::getLearnerId, Function.identity(),
                        (a, b) -> a.getEnrolledOn().isAfter(b.getEnrolledOn()) ? a : b));
        Map<UUID, StreamInfo> streamById = streams.findAll(currentEnrolment.values().stream()
                .map(Enrolment::getStreamId).distinct().toList());
        Map<UUID, GuardianLink> primaryGuardian = guardians.findByLearnerIdIn(ids).stream()
                .sorted(Comparator.comparing(GuardianLink::isPrimaryContact).reversed())
                .collect(Collectors.toMap(GuardianLink::getLearnerId, Function.identity(), (first, second) -> first));
        Map<UUID, PersonRef> contacts = people.findAll(primaryGuardian.values().stream()
                .map(GuardianLink::getPersonId).distinct().toList());
        return learners.stream().map(learner -> {
            Enrolment enrolment = currentEnrolment.get(learner.getId());
            StreamInfo stream = enrolment == null ? null : streamById.get(enrolment.getStreamId());
            GuardianLink link = primaryGuardian.get(learner.getId());
            PersonRef contact = link == null ? null : contacts.get(link.getPersonId());
            return summary(learner, stream, contact);
        }).toList();
    }

    public LearnerSummary summary(Learner learner) {
        return summaries(List.of(learner)).getFirst();
    }

    public LearnerDetail detail(Learner learner) {
        LearnerSummary summary = summary(learner);
        List<GuardianLink> links = guardians.findByLearnerId(learner.getId());
        Map<UUID, PersonRef> persons = people.findAll(links.stream().map(GuardianLink::getPersonId).toList());
        List<LearnerDetail.Guardian> guardianViews = links.stream()
                .filter(link -> persons.containsKey(link.getPersonId()))
                .map(link -> {
                    PersonRef person = persons.get(link.getPersonId());
                    return new LearnerDetail.Guardian(person.id(), person.fullName(), person.phone(), person.email(),
                            link.getRelationship(), link.isFeePayer(), link.isCanPickUp(), link.isPrimaryContact());
                })
                .toList();
        return new LearnerDetail(summary.id(), summary.branchId(), summary.admissionNumber(), summary.givenName(),
                summary.familyName(), summary.fullName(), summary.gradeLevel(), summary.boardingStatus(),
                summary.status(), summary.streamId(), summary.streamName(), summary.primaryContactPhone(),
                learner.getOtherNames(), learner.getDateOfBirth(), learner.getGender(), learner.getUpi(),
                learner.getAssessmentNumber(), learner.getAdmittedOn(), guardianViews);
    }

    private static LearnerSummary summary(Learner learner, StreamInfo stream, PersonRef contact) {
        String fullName = learner.getGivenName() + " " + learner.getFamilyName();
        return new LearnerSummary(learner.getId(), learner.getBranchId(), learner.getAdmissionNumber(),
                learner.getGivenName(), learner.getFamilyName(), fullName, learner.getGradeLevel(),
                learner.getBoardingStatus(), learner.getStatus(),
                stream == null ? null : stream.id(), stream == null ? null : stream.name(),
                contact == null ? null : contact.phone());
    }
}
