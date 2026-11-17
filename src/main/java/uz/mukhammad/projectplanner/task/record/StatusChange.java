package uz.mukhammad.projectplanner.task.record;

import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.enums.TaskStatus;

public record StatusChange(Task task, TaskStatus oldStatus) {}
