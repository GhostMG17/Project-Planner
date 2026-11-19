package uz.mukhammad.projectplanner.risk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.risk.entity.Risk;
import uz.mukhammad.projectplanner.risk.enums.RiskSeverity;
import uz.mukhammad.projectplanner.risk.enums.RiskStatus;
import uz.mukhammad.projectplanner.risk.records.RiskList;
import uz.mukhammad.projectplanner.risk.repository.RiskRepository;
import uz.mukhammad.projectplanner.user.User;
import uz.mukhammad.projectplanner.user.UserService;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskService {

    private final RiskRepository riskRepository;
    private final ProjectService projectService;
    private final UserService userService;

    /** Проверка ДО начала мастера */
    @Transactional(readOnly = true)
    public Project prepareAdd(User actor, Long projectId) {
        Project p = projectService.getById(projectId);
        projectService.requireCanView(actor, p);
        requireOpen(p);
        return p;
    }

    @Transactional
    public Risk createRisk(User actor, Long projectId, String description,
                           RiskSeverity severity, Long ownerId) {
        Project p = prepareAdd(actor, projectId);

        String d = description == null ? "" : description.trim();
        if (d.isEmpty() || d.length() > 500) {
            throw new BotException("Risk description must be 1–500 characters.");
        }

        User owner = userService.getById(ownerId);
        if (!owner.isActive() || !projectService.isOnTeam(p, owner)) {
            throw new BotException(owner.getShortName() + " is not in the " + p.getCode() + " team.");
        }

        Risk risk = riskRepository.save(new Risk(p, d, severity, owner, actor));
        log.info("Risk {} created in {}: severity={}, owner={}, by={}",
                risk.getId(), p.getCode(), severity, owner.getTelegramId(), actor.getTelegramId());
        return risk;
    }

    @Transactional
    public Risk closeRisk(User actor, Long riskId) {
        Risk risk = riskRepository.findWithDetailsById(riskId)
                .orElseThrow(() -> new BotException("Risk not found."));

        boolean allowed = risk.getOwner().getId().equals(actor.getId())
                || projectService.canManage(actor, risk.getProject());
        if (!allowed) {
            throw new BotException("Only the risk owner or PM can close this risk.");
        }
        if (risk.getStatus() == RiskStatus.CLOSED) {
            throw new BotException("This risk is already closed.");
        }

        risk.close();
        log.info("Risk {} in {} closed by {}", risk.getId(), risk.getProject().getCode(), actor.getTelegramId());
        return risk;
    }

    @Transactional(readOnly = true)
    public RiskList getOpenRisks(User actor, Long projectId) {
        Project p = projectService.getById(projectId);
        projectService.requireCanView(actor, p);
        return new RiskList(p, sorted(riskRepository.findByProjectAndStatus(p, RiskStatus.OPEN)));
    }

    @Transactional(readOnly = true)
    public RiskList getOpenRisks(User actor, String projectCode) {
        return getOpenRisks(actor, projectService.getByCode(projectCode).getId());
    }

    /** Все открытые риски во всех доступных проектах — для Portfolio Lead */
    @Transactional(readOnly = true)
    public List<Risk> findOpenVisible(User actor) {
        List<Project> projects = projectService.findVisibleProjects(actor).stream()
                .filter(p -> p.getStatus() == ProjectStatus.ACTIVE || p.getStatus() == ProjectStatus.ON_HOLD)
                .toList();
        if (projects.isEmpty()) {
            return List.of();
        }
        return riskRepository.findByProjectInAndStatus(projects, RiskStatus.OPEN).stream()
                .sorted(Comparator.comparing((Risk r) -> r.getProject().getCode())
                        .thenComparing(Risk::getSeverity, Comparator.reverseOrder())
                        .thenComparing(Risk::getCreatedAt))
                .toList();
    }

    private List<Risk> sorted(List<Risk> risks) {
        return risks.stream()
                .sorted(Comparator.comparing(Risk::getSeverity, Comparator.reverseOrder())
                        .thenComparing(Risk::getCreatedAt))
                .toList();
    }

    private void requireOpen(Project p) {
        if (p.getStatus() != ProjectStatus.ACTIVE && p.getStatus() != ProjectStatus.ON_HOLD) {
            throw new BotException("Project " + p.getCode() + " is " + p.getStatus() + ".");
        }
    }
}
