package uz.mukhammad.projectplanner.task.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.enums.TaskStatus;
import uz.mukhammad.projectplanner.user.User;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
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

    @Query("select coalesce(avg(t.progress), 0) from Task t where t.project = :project")
    Double averageProgress(@Param("project") Project project);

    @Query("select t.project.id, avg(t.progress) from Task t where t.project in :projects group by t.project.id")
    List<Object[]> averageProgressByProjects(@Param("projects") List<Project> projects);

    @EntityGraph(attributePaths = {"project", "project.projectManager", "assignee"})
    List<Task> findByStatusNotAndDeadline(TaskStatus status, LocalDate deadline);

    @EntityGraph(attributePaths = {"project", "project.projectManager", "assignee"})
    List<Task> findByStatusNotAndDeadlineBefore(TaskStatus status, LocalDate date);

    @EntityGraph(attributePaths = {"project", "assignee"})
    List<Task> findByProjectInAndStatusNotAndDeadlineBeforeOrderByDeadlineAsc(
            Collection<Project> projects, TaskStatus status, LocalDate date);

    @Query("select t.status, count(t) from Task t where t.project = :project group by t.status")
    List<Object[]> countByStatus(@Param("project") Project project);

    @EntityGraph(attributePaths = "assignee")
    List<Task> findByProjectAndStatusAndCompletedAtGreaterThanEqualOrderByCompletedAtDesc(
            Project project, TaskStatus status, Instant since);

    @EntityGraph(attributePaths = {"project", "assignee"})
    List<Task> findByProjectInAndStatusNot(Collection<Project> projects, TaskStatus status);

    @EntityGraph(attributePaths = {"project", "assignee"})
    List<Task> findByProjectInAndStatusAndCompletedAtGreaterThanEqualOrderByCompletedAtDesc(
            Collection<Project> projects, TaskStatus status, Instant since);

    @EntityGraph(attributePaths = "assignee")
    List<Task> findByProjectOrderByNumberAsc(Project project);

    long countByProjectInAndStatusAndCompletedAtGreaterThanEqual(
            Collection<Project> projects, TaskStatus status, Instant since);

    long countByProjectAndStatus(Project project, TaskStatus status);

    long countByProjectAndStatusNotAndDeadlineBefore(Project project, TaskStatus status, LocalDate date);
}
