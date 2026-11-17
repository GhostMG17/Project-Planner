package uz.mukhammad.projectplanner.task;

public record StatusChange(Task task, TaskStatus oldStatus) {}
