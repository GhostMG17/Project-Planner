package uz.mukhammad.projectplanner.task;

public record FieldChange(Task task, String oldValue, String newValue) {}
