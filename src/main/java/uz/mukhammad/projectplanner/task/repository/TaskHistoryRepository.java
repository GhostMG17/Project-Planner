package uz.mukhammad.projectplanner.task.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.entity.TaskHistory;

import java.time.Instant;
import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

    @EntityGraph(attributePaths = "changedBy")
    List<TaskHistory> findTop20ByTaskOrderByChangedAtDescIdDesc(Task task);

    @Query("select max(h.changedAt) from TaskHistory h where h.task.project = :project")
    Instant lastActivity(@Param("project") Project project);
}
