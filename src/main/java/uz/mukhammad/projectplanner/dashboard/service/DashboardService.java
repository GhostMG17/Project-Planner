package uz.mukhammad.projectplanner.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mukhammad.projectplanner.config.AppProperties;
import uz.mukhammad.projectplanner.dashboard.records.DailyOverview;
import uz.mukhammad.projectplanner.dashboard.records.WeeklyReport;
import uz.mukhammad.projectplanner.milestone.entity.Milestone;
import uz.mukhammad.projectplanner.milestone.enums.MilestoneStatus;
import uz.mukhammad.projectplanner.milestone.records.MilestoneList;
import uz.mukhammad.projectplanner.milestone.service.MilestoneService;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;
import uz.mukhammad.projectplanner.project.repository.ProjectRepository;
import uz.mukhammad.projectplanner.report.PortfolioService.PortfolioService;
import uz.mukhammad.projectplanner.report.records.PortfolioReport;
import uz.mukhammad.projectplanner.report.records.ProjectSummary;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.enums.TaskStatus;
import uz.mukhammad.projectplanner.task.repository.TaskRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int UPCOMING_DAYS = 3;

    private final PortfolioService portfolioService;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final MilestoneService milestoneService;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public DailyOverview daily() {
        ZoneId zone = appProperties.timezone();
        LocalDate today = LocalDate.now(zone);
        List<Project> projects = activeProjects();
        if (projects.isEmpty()) {
            return new DailyOverview(today, 0, 0, 0, 0, 0, List.of(), List.of());
        }

        List<Task> open = taskRepository.findByProjectInAndStatusNot(projects, TaskStatus.COMPLETED);
        long completedYesterday = taskRepository
                .findByProjectInAndStatusAndCompletedAtGreaterThanEqualOrderByCompletedAtDesc(
                        projects, TaskStatus.COMPLETED, today.minusDays(1).atStartOfDay(zone).toInstant())
                .stream()
                .filter(t -> t.getCompletedAt().isBefore(today.atStartOfDay(zone).toInstant()))
                .count();

        List<Task> upcoming = open.stream()
                .filter(t -> !t.getDeadline().isBefore(today) && !t.getDeadline().isAfter(today.plusDays(UPCOMING_DAYS)))
                .sorted(Comparator.comparing(Task::getDeadline))
                .toList();

        List<ProjectSummary> atRisk = portfolioService.buildSystem().projects().stream()
                .filter(ProjectSummary::needsAttention)
                .toList();

        return new DailyOverview(today, projects.size(), open.size(), completedYesterday,
                open.stream().filter(t -> t.getDeadline().isBefore(today)).count(),
                open.stream().filter(t -> t.getStatus() == TaskStatus.BLOCKED).count(),
                upcoming, atRisk);
    }

    @Transactional(readOnly = true)
    public WeeklyReport weekly() {
        ZoneId zone = appProperties.timezone();
        LocalDate today = LocalDate.now(zone);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate nextMonday = weekStart.plusWeeks(1);
        LocalDate nextSunday = nextMonday.plusDays(6);

        PortfolioReport portfolio = portfolioService.buildSystem();
        List<Project> projects = portfolio.projects().stream().map(ProjectSummary::project).toList();
        if (projects.isEmpty()) {
            return new WeeklyReport(weekStart, today, portfolio, Map.of(), List.of(), List.of(),
                    nextMonday, nextSunday, List.of(), List.of());
        }

        Map<Project, MilestoneList> milestones = new LinkedHashMap<>();
        projects.forEach(p -> milestones.put(p, milestoneService.list(p)));

        List<Task> completed = taskRepository
                .findByProjectInAndStatusAndCompletedAtGreaterThanEqualOrderByCompletedAtDesc(
                        projects, TaskStatus.COMPLETED, weekStart.atStartOfDay(zone).toInstant());

        List<Task> open = taskRepository.findByProjectInAndStatusNot(projects, TaskStatus.COMPLETED);
        List<Task> blocked = open.stream().filter(t -> t.getStatus() == TaskStatus.BLOCKED).toList();
        List<Task> nextWeekTasks = open.stream()
                .filter(t -> !t.getDeadline().isBefore(nextMonday) && !t.getDeadline().isAfter(nextSunday))
                .sorted(Comparator.comparing(Task::getDeadline))
                .toList();
        List<Milestone> nextWeekMilestones = milestones.values().stream()
                .flatMap(ml -> ml.milestones().stream())
                .filter(m -> m.getStatus() == MilestoneStatus.PLANNED)
                .filter(m -> !m.getDueDate().isBefore(nextMonday) && !m.getDueDate().isAfter(nextSunday))
                .sorted(Comparator.comparing(Milestone::getDueDate))
                .toList();

        return new WeeklyReport(weekStart, today, portfolio, milestones, completed, blocked,
                nextMonday, nextSunday, nextWeekTasks, nextWeekMilestones);
    }

    private List<Project> activeProjects() {
        return projectRepository.findByStatusIn(List.of(ProjectStatus.ACTIVE, ProjectStatus.ON_HOLD));
    }
}
