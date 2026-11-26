package uz.mukhammad.projectplanner.milestone.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.mukhammad.projectplanner.milestone.entity.Milestone;
import uz.mukhammad.projectplanner.milestone.enums.MilestoneStatus;
import uz.mukhammad.projectplanner.project.entity.Project;

import java.util.List;
import java.util.Optional;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByProjectOrderByDueDateAscIdAsc(Project project);
    long countByProjectId(Long projectId);
    long countByProjectIdAndStatus(Long projectId, MilestoneStatus status);

    @EntityGraph(attributePaths = {"project", "project.projectManager"})
    Optional<Milestone> findWithProjectById(Long id);
}
