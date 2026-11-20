package uz.mukhammad.projectplanner.report.records;

import uz.mukhammad.projectplanner.risk.entity.Risk;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.user.User;

import java.time.LocalDate;
import java.util.List;

public record PortfolioReport(
        User generatedBy,
        LocalDate today,
        List<ProjectSummary> projects,
        long openTotal,
        long completedLastWeek,
        long blockedTotal,
        List<Task> overdue,
        List<Workload> workload,
        List<Risk> keyRisks,
        long openRisksTotal,
        List<PmCompliance> pmCompliance
) {}
