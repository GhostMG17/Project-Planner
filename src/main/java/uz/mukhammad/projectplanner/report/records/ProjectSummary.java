package uz.mukhammad.projectplanner.report.records;

import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.risk.records.ProjectHealth;

public record ProjectSummary(
        Project project,
        int progress,
        ProjectHealth health,
        long open,
        long overdue,
        long missedMilestones,
        boolean pastEnd
) {
    public boolean needsAttention() {
        return health.atRisk() || missedMilestones > 0 || pastEnd;
    }
}