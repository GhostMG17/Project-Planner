package uz.mukhammad.projectplanner.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mukhammad.projectplanner.config.AppProperties;
import uz.mukhammad.projectplanner.config.WorkloadProperties;
import uz.mukhammad.projectplanner.milestone.entity.Milestone;
import uz.mukhammad.projectplanner.milestone.enums.MilestoneStatus;
import uz.mukhammad.projectplanner.milestone.records.MilestoneList;
import uz.mukhammad.projectplanner.milestone.service.MilestoneService;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.report.entity.ReportLog;
import uz.mukhammad.projectplanner.report.records.ProjectReport;
import uz.mukhammad.projectplanner.report.records.Workload;
import uz.mukhammad.projectplanner.report.repository.ReportLogRepository;
import uz.mukhammad.projectplanner.risk.service.ProjectHealthService;
import uz.mukhammad.projectplanner.risk.service.RiskService;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.enums.TaskStatus;
import uz.mukhammad.projectplanner.task.repository.TaskRepository;
import uz.mukhammad.projectplanner.task.service.ProgressService;
import uz.mukhammad.projectplanner.user.User;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ProjectService projectService;
    private final RiskService riskService;
    private final MilestoneService milestoneService;
    private final ProgressService progressService;
    private final ProjectHealthService healthService;
    private final AppProperties appProperties;
    private final WorkloadProperties workloadProperties;
    private final TaskRepository taskRepository;
    private final ReportLogRepository reportLogRepository;

    @Transactional
    public ProjectReport projectReport(User actor, Long projectId) {
        Project p = projectService.getById(projectId);
        projectService.requireCanManage(actor, p);
        reportLogRepository.save(new ReportLog(p, actor));
        return build(actor, p);
    }

    @Transactional
    public ProjectReport projectReport(User actor, String projectCode) {
        return projectReport(actor, projectService.getByCode(projectCode).getId());
    }

    /** Проекты, по которым пользователь может получить отчёт */
    @Transactional(readOnly = true)
    public List<Project> reportableProjects(User actor) {
        return projectService.findVisibleProjects(actor).stream()
                .filter(p -> p.getStatus() != ProjectStatus.ARCHIVED)
                .filter(p -> projectService.canManage(actor, p))
                .toList();
    }

    private ProjectReport build(User actor, Project p) {
        ZoneId zone = appProperties.timezone();
        LocalDate today = LocalDate.now(zone);

        Map<TaskStatus, Long> distribution = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) {
            distribution.put(s, 0L);
        }
        for (Object[] row : taskRepository.countByStatus(p)) {
            distribution.put((TaskStatus) row[0], (Long) row[1]);
        }

        Instant weekAgo = today.minusDays(7).atStartOfDay(zone).toInstant();
        List<Task> completed = taskRepository
                .findByProjectAndStatusAndCompletedAtGreaterThanEqualOrderByCompletedAtDesc(
                        p, TaskStatus.COMPLETED, weekAgo);

        List<Task> open = taskRepository.findByProjectAndStatusNotOrderByDeadlineAsc(p, TaskStatus.COMPLETED);
        List<Task> overdue = open.stream().filter(t -> t.getDeadline().isBefore(today)).toList();
        List<Task> upcoming = open.stream()
                .filter(t -> !t.getDeadline().isBefore(today) && !t.getDeadline().isAfter(today.plusDays(7)))
                .toList();
        List<Task> blocked = open.stream().filter(t -> t.getStatus() == TaskStatus.BLOCKED).toList();

        List<Workload> workload = projectService.getTeamUsers(actor, p).stream()
                .map(u -> {
                    long userOpen = open.stream().filter(t -> t.getAssignee().getId().equals(u.getId())).count();
                    long userOverdue = overdue.stream().filter(t -> t.getAssignee().getId().equals(u.getId())).count();
                    return new Workload(u, userOpen, userOverdue,
                            userOpen >= workloadProperties.overloadThreshold());
                })
                .sorted(Comparator.comparingLong(Workload::open).reversed())
                .toList();

        LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDate nextSunday = nextMonday.plusDays(6);
        List<Task> nextWeekTasks = open.stream()
                .filter(t -> !t.getDeadline().isBefore(nextMonday) && !t.getDeadline().isAfter(nextSunday))
                .toList();

        MilestoneList milestones = milestoneService.list(p);
        List<Milestone> nextWeekMilestones = milestones.milestones().stream()
                .filter(m -> m.getStatus() == MilestoneStatus.PLANNED)
                .filter(m -> !m.getDueDate().isBefore(nextMonday) && !m.getDueDate().isAfter(nextSunday))
                .toList();

        return new ProjectReport(p, actor, today,
                progressService.progressOf(p), healthService.assess(p), milestones, distribution,
                completed, open, overdue, upcoming, blocked, riskService.openRisks(p), workload,
                nextMonday, nextSunday, nextWeekTasks, nextWeekMilestones);
    }
}
