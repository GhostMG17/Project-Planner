package uz.mukhammad.projectplanner.task.record;

import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.entity.TaskHistory;

import java.util.List;

public record TaskWithHistory(Task task, List<TaskHistory> history) {}
