package uz.mukhammad.projectplanner.task.record;

import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;

public record ReassignOptions(Task task, List<User> candidates) {}