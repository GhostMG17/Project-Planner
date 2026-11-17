package uz.mukhammad.projectplanner.task.record;

import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.user.User;

public record Reassignment(Task task, User oldAssignee) {}
