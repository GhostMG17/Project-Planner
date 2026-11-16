package uz.mukhammad.projectplanner.task;

import java.time.LocalDate;

public record CreateTaskRequest(
        String projectCode,
        String title,
        String assigneeUsername,
        LocalDate deadline,
        TaskPriority priority,
        String deliverable
) {}
