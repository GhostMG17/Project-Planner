package uz.mukhammad.projectplanner.milestone.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.config.AppProperties;
import uz.mukhammad.projectplanner.milestone.entity.Milestone;
import uz.mukhammad.projectplanner.milestone.records.MilestoneList;
import uz.mukhammad.projectplanner.milestone.repository.MilestoneRepository;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.user.User;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final ProjectService projectService;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public MilestoneList getMilestones(User actor, Long projectId) {
        Project p = projectService.getById(projectId);
        projectService.requireCanView(actor, p);
        return list(p);
    }

    /** Для отчётов — без проверки прав, их проверяет вызывающий */
    @Transactional(readOnly = true)
    public MilestoneList list(Project p) {
        return new MilestoneList(p, milestoneRepository.findByProjectOrderByDueDateAscIdAsc(p), today());
    }

    @Transactional(readOnly = true)
    public Project prepareAdd(User actor, Long projectId) {
        Project p = projectService.getById(projectId);
        projectService.requireCanManage(actor, p);
        requireOpen(p);
        return p;
    }

    @Transactional
    public Milestone addMilestone(User actor, Long projectId, String name, LocalDate dueDate) {
        Project p = prepareAdd(actor, projectId);

        String n = name == null ? "" : name.trim();
        if (n.isEmpty() || n.length() > 200) {
            throw new BotException("Milestone name must be 1–200 characters.");
        }
        validateDate(p, dueDate);

        Milestone m = milestoneRepository.save(new Milestone(p, n, dueDate, actor));
        log.info("Milestone {} added to {} by {}", m.getId(), p.getCode(), actor.getTelegramId());
        return m;
    }

    /** Проверка даты — вызывается и в мастере сразу, и при сохранении */
    public void validateDate(Project p, LocalDate dueDate) {
        if (dueDate.isBefore(today())) {
            throw new BotException("Milestone date can't be in the past.");
        }
        if (dueDate.isBefore(p.getStartDate()) || dueDate.isAfter(p.getPlannedEndDate())) {
            throw new BotException("Milestone date must be within the project period ("
                    + p.getStartDate().format(DateParser.FORMAT) + " → "
                    + p.getPlannedEndDate().format(DateParser.FORMAT) + ").\n"
                    + "If the project is extended, change its end date first: /project " + p.getCode() + " → Edit.");
        }
    }

    @Transactional
    public Milestone toggle(User actor, Long milestoneId) {
        Milestone m = milestoneRepository.findWithProjectById(milestoneId)
                .orElseThrow(() -> new BotException("Milestone not found."));
        projectService.requireCanManage(actor, m.getProject());
        requireOpen(m.getProject());

        m.toggle();
        log.info("Milestone {} in {} -> {} by {}",
                m.getId(), m.getProject().getCode(), m.getStatus(), actor.getTelegramId());
        return m;
    }

    private LocalDate today() {
        return LocalDate.now(appProperties.timezone());
    }

    private void requireOpen(Project p) {
        if (p.getStatus() != ProjectStatus.ACTIVE && p.getStatus() != ProjectStatus.ON_HOLD) {
            throw new BotException("Project " + p.getCode() + " is " + p.getStatus() + ".");
        }
    }
}
