package uz.mukhammad.projectplanner.task;

import java.util.List;

public record TaskWithHistory(Task task, List<TaskHistory> history) {}
