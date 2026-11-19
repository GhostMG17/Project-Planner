package uz.mukhammad.projectplanner.risk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mukhammad.projectplanner.config.AppProperties;
import uz.mukhammad.projectplanner.config.RiskProperties;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.risk.ProjectHealth;
import uz.mukhammad.projectplanner.task.enums.TaskStatus;
import uz.mukhammad.projectplanner.task.repository.TaskRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProjectHealthService {

    private final TaskRepository taskRepository;
    private final AppProperties appProperties;
    private final RiskProperties riskProperties;

    @Transactional(readOnly = true)
    public ProjectHealth assess(Project p) {
        LocalDate today = LocalDate.now(appProperties.timezone());
        long overdue = taskRepository.countByProjectAndStatusNotAndDeadlineBefore(p, TaskStatus.COMPLETED, today);
        long blocked = taskRepository.countByProjectAndStatus(p, TaskStatus.BLOCKED);
        boolean auto = overdue + blocked >= riskProperties.threshold();
        return new ProjectHealth(p.isAtRiskManual() || auto, p.isAtRiskManual(), overdue, blocked);
    }
}
