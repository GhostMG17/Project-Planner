package uz.mukhammad.projectplanner.task.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.task.repository.TaskRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public int progressOf(Project project) {
        return (int) Math.round(taskRepository.averageProgress(project));
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> progressOf(List<Project> projects) {
        if (projects.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : taskRepository.averageProgressByProjects(projects)) {
            result.put((Long) row[0], (int) Math.round((Double) row[1]));
        }
        return result;
    }
}
