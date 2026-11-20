package uz.mukhammad.projectplanner.report.records;

import uz.mukhammad.projectplanner.milestone.entity.Milestone;
import uz.mukhammad.projectplanner.milestone.records.MilestoneList;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.risk.entity.Risk;
import uz.mukhammad.projectplanner.risk.records.ProjectHealth;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.enums.TaskStatus;
import uz.mukhammad.projectplanner.user.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ProjectReport(
        Project project,
        User generatedBy,
        LocalDate today,
        int progress,
        ProjectHealth health,
        MilestoneList milestones,
        Map<TaskStatus, Long> distribution,
        List<Task> completedLastWeek,
        List<Task> open,
        List<Task> overdue,
        List<Task> upcoming,
        List<Task> blocked,
        List<Risk> risks,
        List<Workload> workload,
        LocalDate nextWeekStart,
        LocalDate nextWeekEnd,
        List<Task> nextWeekTasks,
        List<Milestone> nextWeekMilestones
) {}
