package uz.mukhammad.projectplanner.dashboard.records;

import uz.mukhammad.projectplanner.milestone.entity.Milestone;
import uz.mukhammad.projectplanner.milestone.records.MilestoneList;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.report.records.PortfolioReport;
import uz.mukhammad.projectplanner.task.entity.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record WeeklyReport(
        LocalDate weekStart,
        LocalDate today,
        PortfolioReport portfolio,
        Map<Project, MilestoneList> milestones,
        List<Task> completedThisWeek,
        List<Task> blocked,
        LocalDate nextWeekStart,
        LocalDate nextWeekEnd,
        List<Task> nextWeekTasks,
        List<Milestone> nextWeekMilestones
) {}