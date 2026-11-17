package uz.mukhammad.projectplanner.task.record;

import uz.mukhammad.projectplanner.task.enums.FileType;

public record NewComment(String body, String fileId, FileType fileType, String fileName) {}
