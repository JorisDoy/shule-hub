package ke.shulehub.fees;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ke.shulehub.academics.AcademicCalendar;
import ke.shulehub.fees.FeeViews.FeeItemView;
import ke.shulehub.fees.FeeViews.FeeStructureView;
import ke.shulehub.fees.FeeViews.VoteHeadView;
import ke.shulehub.fees.internal.FeeStructure;
import ke.shulehub.fees.internal.FeeStructureItem;
import ke.shulehub.fees.internal.FeeStructureRepository;
import ke.shulehub.fees.internal.VoteHead;
import ke.shulehub.fees.internal.VoteHeadRepository;
import ke.shulehub.identity.AccessPolicy;
import ke.shulehub.identity.Permission;
import ke.shulehub.learners.BoardingStatus;
import ke.shulehub.shared.money.Money;
import ke.shulehub.shared.school.GradeLevel;
import ke.shulehub.shared.web.BadRequestException;
import ke.shulehub.shared.web.ConflictException;
import ke.shulehub.shared.web.NotFoundException;
import ke.shulehub.tenancy.BranchDirectory;
import ke.shulehub.tenancy.TenantProvisioned;

/** Vote heads and per-term fee structures. */
@Service
public class FeeSetup {

    public record ItemInput(UUID voteHeadId, BigDecimal amount, FeeAppliesTo appliesTo) {
    }

    private final VoteHeadRepository voteHeads;
    private final FeeStructureRepository structures;
    private final BranchDirectory branches;
    private final AcademicCalendar calendar;
    private final AccessPolicy access;

    public FeeSetup(VoteHeadRepository voteHeads, FeeStructureRepository structures, BranchDirectory branches,
            AcademicCalendar calendar, AccessPolicy access) {
        this.voteHeads = voteHeads;
        this.structures = structures;
        this.branches = branches;
        this.calendar = calendar;
        this.access = access;
    }

    /** Default vote heads for a new school; editable later. Lower priority is paid first. */
    @EventListener
    @Transactional
    public void seedDefaults(TenantProvisioned event) {
        List<VoteHead> defaults = List.of(
                new VoteHead("TUITION", "Tuition", 10),
                new VoteHead("BOARDING", "Boarding", 20),
                new VoteHead("LUNCH", "Lunch", 30),
                new VoteHead("TRANSPORT", "Transport", 40),
                new VoteHead("ACTIVITY", "Activity", 50),
                new VoteHead("ADMISSION", "Admission fee", 60));
        defaults.stream().filter(head -> !voteHeads.existsByCode(head.getCode())).forEach(voteHeads::save);
    }

    @Transactional(readOnly = true)
    public List<VoteHeadView> voteHeads() {
        return voteHeads.findAllByOrderByPriorityAscNameAsc().stream().map(FeeSetup::view).toList();
    }

    @Transactional
    public VoteHeadView createVoteHead(String code, String name, int priority) {
        access.requireTenantWide(Permission.FEES_MANAGE);
        VoteHead head = new VoteHead(code, name, priority);
        if (voteHeads.existsByCode(head.getCode())) {
            throw new ConflictException("Vote head " + head.getCode() + " already exists");
        }
        return view(voteHeads.save(head));
    }

    @Transactional(readOnly = true)
    public List<FeeStructureView> structures(UUID branchId, UUID termId) {
        branches.require(branchId);
        access.require(Permission.FEES_VIEW, branchId);
        List<FeeStructure> found = termId == null ? structures.findWithItemsByBranch(branchId)
                : structures.findWithItems(branchId, termId);
        Map<UUID, VoteHead> heads = voteHeadsById();
        return found.stream()
                .sorted(Comparator.comparing(FeeStructure::getGradeLevel))
                .map(structure -> view(structure, heads))
                .toList();
    }

    @Transactional
    public FeeStructureView create(UUID branchId, UUID termId, GradeLevel gradeLevel, String name,
            List<ItemInput> items) {
        branches.require(branchId);
        access.require(Permission.FEES_MANAGE, branchId);
        calendar.requireTerm(termId);
        FeeStructure structure = new FeeStructure(branchId, termId, gradeLevel, name);
        structure.replaceItems(name, drafts(items));
        try {
            return view(structures.saveAndFlush(structure), voteHeadsById());
        }
        catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException("There is already a fee structure for " + gradeLevel.label() + " this term");
        }
    }

    @Transactional
    public FeeStructureView update(UUID structureId, String name, List<ItemInput> items) {
        FeeStructure structure = load(structureId);
        access.require(Permission.FEES_MANAGE, structure.getBranchId());
        structure.replaceItems(name, drafts(items));
        return view(structure, voteHeadsById());
    }

    @Transactional
    public FeeStructureView publish(UUID structureId) {
        FeeStructure structure = load(structureId);
        access.require(Permission.FEES_MANAGE, structure.getBranchId());
        structure.publish();
        return view(structure, voteHeadsById());
    }

    private List<FeeStructureItem.Draft> drafts(List<ItemInput> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Add at least one fee item");
        }
        Set<UUID> known = voteHeadsById().keySet();
        return items.stream().map(item -> {
            if (!known.contains(item.voteHeadId())) {
                throw new BadRequestException("Unknown vote head");
            }
            if (!Money.isPositive(item.amount())) {
                throw new BadRequestException("Fee amounts must be greater than zero");
            }
            return new FeeStructureItem.Draft(item.voteHeadId(), item.amount(),
                    item.appliesTo() == null ? FeeAppliesTo.ALL : item.appliesTo());
        }).toList();
    }

    private FeeStructure load(UUID structureId) {
        return structures.findById(structureId).orElseThrow(() -> new NotFoundException("Fee structure not found"));
    }

    private Map<UUID, VoteHead> voteHeadsById() {
        return voteHeads.findAll().stream().collect(Collectors.toMap(VoteHead::getId, Function.identity()));
    }

    private static VoteHeadView view(VoteHead head) {
        return new VoteHeadView(head.getId(), head.getCode(), head.getName(), head.getPriority());
    }

    static FeeStructureView view(FeeStructure structure, Map<UUID, VoteHead> heads) {
        List<FeeItemView> items = structure.getItems().stream()
                .sorted(Comparator.comparingInt(item -> heads.containsKey(item.getVoteHeadId())
                        ? heads.get(item.getVoteHeadId()).getPriority() : Integer.MAX_VALUE))
                .map(item -> new FeeItemView(item.getId(), item.getVoteHeadId(),
                        heads.containsKey(item.getVoteHeadId()) ? heads.get(item.getVoteHeadId()).getName() : "?",
                        item.getAmount(), item.getAppliesTo()))
                .toList();
        return new FeeStructureView(structure.getId(), structure.getBranchId(), structure.getTermId(),
                structure.getGradeLevel(), structure.getName(), structure.getStatus(),
                structure.totalFor(BoardingStatus.DAY), structure.totalFor(BoardingStatus.BOARDING), items);
    }
}
