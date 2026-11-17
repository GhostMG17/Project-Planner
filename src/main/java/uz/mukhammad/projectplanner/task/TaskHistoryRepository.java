package uz.mukhammad.projectplanner.task;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

    @EntityGraph(attributePaths = "changedBy")
    List<TaskHistory> findTop20ByTaskOrderByChangedAtDescIdDesc(Task task);
}
