package uz.mukhammad.projectplanner.task.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.entity.TaskComment;

import java.util.List;
import java.util.Optional;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    @EntityGraph(attributePaths = "author")
    List<TaskComment> findTop10ByTaskOrderByCreatedAtDescIdDesc(Task task);

    long countByTask(Task task);

    @EntityGraph(attributePaths = {"task", "task.project", "task.project.projectManager"})
    Optional<TaskComment> findWithTaskById(Long id);
}