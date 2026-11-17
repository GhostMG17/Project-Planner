package uz.mukhammad.projectplanner.task;

import uz.mukhammad.projectplanner.user.User;

import java.util.List;

public record ReassignOptions(Task task, List<User> candidates) {}