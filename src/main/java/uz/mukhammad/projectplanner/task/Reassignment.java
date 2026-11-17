package uz.mukhammad.projectplanner.task;

import uz.mukhammad.projectplanner.user.User;

public record Reassignment(Task task, User oldAssignee) {}
