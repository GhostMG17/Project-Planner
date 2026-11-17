package uz.mukhammad.projectplanner.project.record;

import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;

public record ProjectTeam(Project project, List<User> members) {}
