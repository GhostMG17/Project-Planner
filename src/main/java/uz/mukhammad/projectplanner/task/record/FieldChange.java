package uz.mukhammad.projectplanner.task.record;

import uz.mukhammad.projectplanner.task.entity.Task;

public record FieldChange(Task task, String oldValue, String newValue) {}
