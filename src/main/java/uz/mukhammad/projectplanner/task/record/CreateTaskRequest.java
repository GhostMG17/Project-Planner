package uz.mukhammad.projectplanner.task.record;

import uz.mukhammad.projectplanner.task.enums.TaskPriority;

import java.time.LocalDate;

public record CreateTaskRequest(
        String projectCode,
        String title,
        Long assigneeId,
        LocalDate deadline,
        TaskPriority priority,
        String deliverable
) {}