package uz.mukhammad.projectplanner.project.record;

public record ProjectSummary(long members,
                             long milestonesDone,
                             long milestonesTotal,
                             long openRisks,
                             long highRisks) {
}