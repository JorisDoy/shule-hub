package ke.shulehub.academics;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.academics.internal.AcademicYearRepository;
import ke.shulehub.academics.internal.Stream;
import ke.shulehub.academics.internal.StreamRepository;
import ke.shulehub.identity.AccessPolicy;
import ke.shulehub.identity.Permission;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.web.BadRequestException;
import ke.shulehub.shared.web.NotFoundException;
import ke.shulehub.tenancy.BranchDirectory;

/** Classes (streams) per branch and academic year. */
@Service
public class Streams {

    private final StreamRepository streams;
    private final AcademicYearRepository years;
    private final BranchDirectory branches;
    private final AccessPolicy access;

    public Streams(StreamRepository streams, AcademicYearRepository years, BranchDirectory branches,
            AccessPolicy access) {
        this.streams = streams;
        this.years = years;
        this.branches = branches;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<StreamInfo> list(UUID branchId, UUID academicYearId) {
        branches.require(branchId);
        List<Stream> found = academicYearId == null
                ? streams.findByBranchIdOrderByGradeLevelAscNameAsc(branchId)
                : streams.findByBranchIdAndAcademicYearIdOrderByGradeLevelAscNameAsc(branchId, academicYearId);
        return found.stream()
                .map(Stream::toInfo)
                .sorted(Comparator.comparing(StreamInfo::gradeLevel).thenComparing(StreamInfo::name))
                .toList();
    }

    @Transactional
    public StreamInfo create(UUID branchId, UUID academicYearId, GradeLevel gradeLevel, String name, int capacity) {
        access.require(Permission.ACADEMICS_MANAGE, branchId);
        branches.require(branchId);
        if (!years.existsById(academicYearId)) {
            throw new NotFoundException("Academic year not found");
        }
        if (capacity < 1 || capacity > 200) {
            throw new BadRequestException("Capacity must be between 1 and 200");
        }
        return streams.save(new Stream(branchId, academicYearId, gradeLevel, name, capacity)).toInfo();
    }

    @Transactional(readOnly = true)
    public Optional<StreamInfo> find(UUID streamId) {
        return streams.findById(streamId).map(Stream::toInfo);
    }

    @Transactional(readOnly = true)
    public StreamInfo require(UUID streamId) {
        if (streamId == null) {
            throw new NotFoundException("Choose a class");
        }
        return streams.findById(streamId).map(Stream::toInfo).orElseThrow(() -> new NotFoundException("Class not found"));
    }

    @Transactional(readOnly = true)
    public Map<UUID, StreamInfo> findAll(Collection<UUID> streamIds) {
        return streams.findAllById(streamIds).stream()
                .map(Stream::toInfo)
                .collect(Collectors.toMap(StreamInfo::id, Function.identity()));
    }
}
