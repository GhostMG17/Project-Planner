package uz.mukhammad.projectplanner.report.PortfolioService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mukhammad.projectplanner.config.AppProperties;
import uz.mukhammad.projectplanner.config.WorkloadProperties;
import uz.mukhammad.projectplanner.milestone.service.MilestoneService;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;
import uz.mukhammad.projectplanner.project.repository.ProjectRepository;
import uz.mukhammad.projectplanner.report.entity.ReportLog;
import uz.mukhammad.projectplanner.report.records.PmCompliance;
import uz.mukhammad.projectplanner.report.records.PortfolioReport;
import uz.mukhammad.projectplanner.report.records.ProjectSummary;
import uz.mukhammad.projectplanner.report.records.Workload;
import uz.mukhammad.projectplanner.report.repository.ReportLogRepository;
import uz.mukhammad.projectplanner.risk.entity.Risk;
import uz.mukhammad.projectplanner.risk.enums.RiskSeverity;
import uz.mukhammad.projectplanner.risk.enums.RiskStatus;
import uz.mukhammad.projectplanner.risk.repository.RiskRepository;
import uz.mukhammad.projectplanner.risk.service.ProjectHealthService;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.enums.TaskStatus;
import uz.mukhammad.projectplanner.task.repository.TaskHistoryRepository;
import uz.mukhammad.projectplanner.task.repository.TaskRepository;
import uz.mukhammad.projectplanner.task.service.ProgressService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;
import uz.mukhammad.projectplanner.user.UserService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final int COMPLIANCE_DAYS = 7;

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskHistoryRepository historyRepository;
    private final RiskRepository riskRepository;
    private final ReportLogRepository reportLogRepository;
    private final UserService userService;
    private final ProgressService progressService;
    private final ProjectHealthService healthService;
    private final MilestoneService milestoneService;
    private final AccessService accessService;
    private final AppProperties appProperties;
    private final WorkloadProperties workloadProperties;

    @Transactional(readOnly = true)
    public PortfolioReport build(User actor) {
        accessService.requireRole(actor, Role.ADMIN, Role.PORTFOLIO_LEAD);
        return compute(actor);
    }

    @Transactional(readOnly = true)
    public PortfolioReport buildSystem() {
        return compute(null);
    }

    private PortfolioReport compute(User generatedBy) {
        ZoneId zone = appProperties.timezone();
        LocalDate today = LocalDate.now(zone);
        Instant weekAgo = today.minusDays(COMPLIANCE_DAYS).atStartOfDay(zone).toInstant();

        List<Project> projects = projectRepository.findByStatusIn(
                List.of(ProjectStatus.ACTIVE, ProjectStatus.ON_HOLD));
        if (projects.isEmpty()) {
            return new PortfolioReport(generatedBy, today, List.of(), 0, 0, 0,
                    List.of(), List.of(), List.of(), 0, List.of());
        }

        List<Task> open = taskRepository.findByProjectInAndStatusNot(projects, TaskStatus.COMPLETED);
        List<Task> overdue = open.stream()
                .filter(t -> t.getDeadline().isBefore(today))
                .sorted(Comparator.comparing(Task::getDeadline))
                .toList();
        long blocked = open.stream().filter(t -> t.getStatus() == TaskStatus.BLOCKED).count();
        long completedLastWeek = taskRepository.countByProjectInAndStatusAndCompletedAtGreaterThanEqual(
                projects, TaskStatus.COMPLETED, weekAgo);

        Map<Long, Integer> progress = progressService.progressOf(projects);
        List<ProjectSummary> summaries = projects.stream()
                .map(p -> new ProjectSummary(
                        p,
                        progress.getOrDefault(p.getId(), 0),
                        healthService.assess(p),
                        open.stream().filter(t -> t.getProject().getId().equals(p.getId())).count(),
                        overdue.stream().filter(t -> t.getProject().getId().equals(p.getId())).count(),
                        milestoneService.list(p).missed(),
                        p.getPlannedEndDate().isBefore(today)))
                .sorted(Comparator.comparing((ProjectSummary s) -> !s.needsAttention())
                        .thenComparing(s -> s.project().getCode()))
                .toList();

        List<Workload> workload = userService.findAllActive().stream()
                .map(u -> {
                    long userOpen = open.stream().filter(t -> t.getAssignee().getId().equals(u.getId())).count();
                    long userOverdue = overdue.stream().filter(t -> t.getAssignee().getId().equals(u.getId())).count();
                    return new Workload(u, userOpen, userOverdue,
                            userOpen >= workloadProperties.overloadThreshold());
                })
                .filter(w -> w.open() > 0)
                .sorted(Comparator.comparingLong(Workload::open).reversed())
                .toList();

        List<Risk> openRisks = riskRepository.findByProjectInAndStatus(projects, RiskStatus.OPEN);
        List<Risk> keyRisks = openRisks.stream()
                .filter(r -> r.getSeverity() == RiskSeverity.HIGH || r.getSeverity() == RiskSeverity.CRITICAL)
                .sorted(Comparator.comparing(Risk::getSeverity, Comparator.reverseOrder())
                        .thenComparing(r -> r.getProject().getCode()))
                .toList();

        List<PmCompliance> compliance = projects.stream()
                .map(p -> {
                    User pm = p.getProjectManager();
                    Instant lastReport = reportLogRepository.findTopByProjectAndUserOrderByCreatedAtDesc(p, pm)
                            .map(ReportLog::getCreatedAt).orElse(null);
                    return new PmCompliance(p, pm, lastReport, historyRepository.lastActivity(p),
                            lastReport != null && !lastReport.isBefore(weekAgo));
                })
                .sorted(Comparator.comparing(PmCompliance::compliant)
                        .thenComparing(c -> c.project().getCode()))
                .toList();

        return new PortfolioReport(generatedBy, today, summaries, open.size(), completedLastWeek, blocked,
                overdue, workload, keyRisks, openRisks.size(), compliance);
    }
}
