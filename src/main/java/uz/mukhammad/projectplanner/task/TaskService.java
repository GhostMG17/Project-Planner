package uz.mukhammad.projectplanner.task;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.config.AppProperties;
import uz.mukhammad.projectplanner.project.Project;
import uz.mukhammad.projectplanner.project.ProjectService;
import uz.mukhammad.projectplanner.project.ProjectStatus;
import uz.mukhammad.projectplanner.user.User;
import uz.mukhammad.projectplanner.user.UserService;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final AppProperties appProperties;

    @Transactional
    public Task createTask(User actor, CreateTaskRequest req) {
        Project project = projectService.getByCode(req.projectCode());
        projectService.requireCanView(actor, project);

        if (project.getStatus() != ProjectStatus.ACTIVE && project.getStatus() != ProjectStatus.ON_HOLD) {
            throw new BotException("Project " + project.getCode() + " is " + project.getStatus()
                    + ". New tasks can't be added.");
        }

        User assignee = userService.getByUsername(req.assigneeUsername());
        if (!projectService.isOnTeam(project, assignee)) {
            throw new BotException("@" + assignee.getUsername() + " is not in the " + project.getCode()
                    + " team. Add them first: /addmember " + project.getCode() + " @" + assignee.getUsername());
        }

        String title = req.title().trim();
        if (title.isEmpty() || title.length() > 300) {
            throw new BotException("Task title must be 1–300 characters.");
        }

        String deliverable = req.deliverable() == null || req.deliverable().isBlank()
                ? null : req.deliverable().trim();
        if (deliverable != null && deliverable.length() > 500) {
            throw new BotException("Deliverable is too long (max 500 characters).");
        }

        LocalDate today = LocalDate.now(appProperties.timezone());
        if (req.deadline().isBefore(today)) {
            throw new BotException("Deadline can't be in the past.");
        }

        Task task = new Task();
        task.setProject(project);
        task.setTitle(title);
        task.setDeliverable(deliverable);
        task.setAssignee(assignee);
        task.setPriority(req.priority());
        task.setStartDate(today);
        task.setDeadline(req.deadline());
        task.setCreatedBy(actor);

        task = taskRepository.save(task);
        log.info("Task created: {} in {}, assignee={}, by={}",
                task.getKey(), project.getCode(), assignee.getTelegramId(), actor.getTelegramId());
        return task;
    }
}
