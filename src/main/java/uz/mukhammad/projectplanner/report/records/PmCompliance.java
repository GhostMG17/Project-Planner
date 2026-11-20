package uz.mukhammad.projectplanner.report.records;

import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.user.User;

import java.time.Instant;

public record PmCompliance(Project project, User pm, Instant lastReport, Instant lastActivity, boolean compliant) {}
