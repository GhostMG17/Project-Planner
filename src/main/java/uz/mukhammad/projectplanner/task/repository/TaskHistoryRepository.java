package uz.mukhammad.projectplanner.task.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.entity.TaskHistory;

import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

    @EntityGraph(attributePaths = "changedBy")
    List<TaskHistory> findTop20ByTaskOrderByChangedAtDescIdDesc(Task task);
}
