package uz.mukhammad.projectplanner.milestone.records;

import uz.mukhammad.projectplanner.milestone.entity.Milestone;
import uz.mukhammad.projectplanner.milestone.enums.MilestoneStatus;
import uz.mukhammad.projectplanner.project.entity.Project;

import java.time.LocalDate;
import java.util.List;

public record MilestoneList(Project project, List<Milestone> milestones, LocalDate today) {

    public long completed() {
        return milestones.stream().filter(m -> m.getStatus() == MilestoneStatus.COMPLETED).count();
    }

    public long missed() {
        return milestones.stream().filter(m -> m.isMissed(today)).count();
    }
}
