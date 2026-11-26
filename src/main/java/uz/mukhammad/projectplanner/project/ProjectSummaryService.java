package uz.mukhammad.projectplanner.project;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mukhammad.projectplanner.milestone.enums.MilestoneStatus;
import uz.mukhammad.projectplanner.milestone.repository.MilestoneRepository;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.record.ProjectSummary;
import uz.mukhammad.projectplanner.project_member.ProjectMemberRepository;
import uz.mukhammad.projectplanner.risk.enums.RiskSeverity;
import uz.mukhammad.projectplanner.risk.enums.RiskStatus;
import uz.mukhammad.projectplanner.risk.repository.RiskRepository;

import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class ProjectSummaryService {

    private final ProjectMemberRepository memberRepository;
    private final MilestoneRepository milestoneRepository;
    private final RiskRepository riskRepository;

    @Transactional(readOnly = true)
    public ProjectSummary summarize(Project p) {
        Long id = p.getId();
        return new ProjectSummary(
                memberRepository.countByProjectId(id),
                milestoneRepository.countByProjectIdAndStatus(id, MilestoneStatus.COMPLETED),
                milestoneRepository.countByProjectId(id),
                riskRepository.countByProjectIdAndStatus(id, RiskStatus.OPEN),
                riskRepository.countByProjectIdAndStatusAndSeverityIn(
                        id, RiskStatus.OPEN, EnumSet.of(RiskSeverity.HIGH, RiskSeverity.CRITICAL)));
    }
}
