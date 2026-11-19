package uz.mukhammad.projectplanner.risk.records;

import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.risk.entity.Risk;

import java.util.List;

public record RiskList(Project project, List<Risk> risks) {}