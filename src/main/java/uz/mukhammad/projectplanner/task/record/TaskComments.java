package uz.mukhammad.projectplanner.task.record;

import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.entity.TaskComment;

import java.util.List;

public record TaskComments(Task task, List<TaskComment> comments, long total) {}