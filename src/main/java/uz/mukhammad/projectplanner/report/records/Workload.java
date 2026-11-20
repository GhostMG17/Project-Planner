package uz.mukhammad.projectplanner.report.records;

import uz.mukhammad.projectplanner.user.User;

public record Workload(User user, long open, long overdue, boolean overloaded) {}
