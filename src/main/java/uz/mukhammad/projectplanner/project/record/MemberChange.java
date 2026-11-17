package uz.mukhammad.projectplanner.project.record;

import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.user.User;

public record MemberChange(Project project, User user) {}
