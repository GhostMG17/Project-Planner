package uz.mukhammad.projectplanner.risk.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.risk.entity.Risk;
import uz.mukhammad.projectplanner.risk.enums.RiskSeverity;
import uz.mukhammad.projectplanner.risk.enums.RiskStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RiskRepository extends JpaRepository<Risk, Long> {

    @EntityGraph(attributePaths = "owner")
    List<Risk> findByProjectAndStatus(Project project, RiskStatus status);

    @EntityGraph(attributePaths = {"owner", "project"})
    List<Risk> findByProjectInAndStatus(Collection<Project> projects, RiskStatus status);

    @EntityGraph(attributePaths = {"owner", "project", "project.projectManager"})
    Optional<Risk> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = "owner")
    List<Risk> findByProjectOrderByCreatedAtAsc(Project project);

    long countByProjectIdAndStatus(Long projectId, RiskStatus status);
    long countByProjectIdAndStatusAndSeverityIn(Long projectId, RiskStatus status, Collection<RiskSeverity> severities);
}