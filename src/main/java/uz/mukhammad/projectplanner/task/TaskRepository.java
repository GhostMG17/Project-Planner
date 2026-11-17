package uz.mukhammad.projectplanner.task;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.mukhammad.projectplanner.project.Project;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"project", "project.projectManager", "assignee"})
    Optional<Task> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"project", "project.projectManager", "assignee"})
    Optional<Task> findByNumber(Long number);

    @EntityGraph(attributePaths = {"project", "assignee"})
    List<Task> findByAssigneeAndStatusNotOrderByDeadlineAsc(User assignee, TaskStatus status);

    @EntityGraph(attributePaths = {"project", "assignee"})
    List<Task> findByProjectAndStatusNotOrderByDeadlineAsc(Project project, TaskStatus status);

    long countByProjectAndStatus(Project project, TaskStatus status);
}
