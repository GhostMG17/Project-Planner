package uz.mukhammad.projectplanner.dashboard.records;

import uz.mukhammad.projectplanner.report.records.ProjectSummary;
import uz.mukhammad.projectplanner.task.entity.Task;

import java.time.LocalDate;
import java.util.List;

public record DailyOverview(
        LocalDate today,
        int activeProjects,
        long openTasks,
        long completedYesterday,
        long overdue,
        long blocked,
        List<Task> upcoming,
        List<ProjectSummary> atRisk
) {}
