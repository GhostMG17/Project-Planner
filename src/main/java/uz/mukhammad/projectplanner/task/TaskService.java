package uz.mukhammad.projectplanner.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.config.AppProperties;
import uz.mukhammad.projectplanner.project.Project;
import uz.mukhammad.projectplanner.project.ProjectService;
import uz.mukhammad.projectplanner.project.ProjectStatus;
import uz.mukhammad.projectplanner.user.User;
import uz.mukhammad.projectplanner.user.UserService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskHistoryRepository historyRepository;
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
        record(task, actor, ChangeType.CREATED, null, task.getStatus(),
                "Assigned to " + assignee.getShortName());
        log.info("Task created: {} in {}, assignee={}, by={}",
                task.getKey(), project.getCode(), assignee.getTelegramId(), actor.getTelegramId());
        return task;
    }


    @Transactional(readOnly = true)
    public Task getForView(User actor, Long taskId) {
        Task task = getById(taskId);
        projectService.requireCanView(actor, task.getProject());
        return task;
    }

    @Transactional(readOnly = true)
    public Task getForViewByKey(User actor, String rawKey) {
        Long number = parseKey(rawKey);
        Task task = taskRepository.findByNumber(number)
                .orElseThrow(() -> new BotException("Task " + rawKey.toUpperCase() + " not found."));
        projectService.requireCanView(actor, task.getProject());
        return task;
    }

    @Transactional(readOnly = true)
    public Task prepareStatusChange(User actor, Long taskId, TaskStatus target) {
        Task task = getById(taskId);
        checkTransition(actor, task, target);
        return task;
    }

    @Transactional
    public StatusChange changeStatus(User actor, Long taskId, TaskStatus target, String reason) {
        Task task = getById(taskId);
        checkTransition(actor, task, target);
        TaskStatus old = task.getStatus();
        int oldProgress = task.getProgress();

        if (target == TaskStatus.BLOCKED) {
            String r = reason == null ? "" : reason.trim();
            if (r.isEmpty()) {
                throw new BotException("Please describe why the task is blocked.");
            }
            if (r.length() > 500) {
                throw new BotException("Reason is too long (max 500 characters).");
            }
            task.setBlockedReason(r);
        } else {
            task.setBlockedReason(null);
        }

        if (target == TaskStatus.COMPLETED) {
            task.setProgress(100);
            task.setCompletedAt(Instant.now());
        } else {
            task.setCompletedAt(null);
            if (task.getProgress() == 100) {
                task.setProgress(90);
            }
        }

        task.setStatus(target);

        record(task, actor, ChangeType.STATUS, old, target,
                target == TaskStatus.BLOCKED ? task.getBlockedReason() : null);
        if (oldProgress != task.getProgress()) {
            record(task, actor, ChangeType.PROGRESS, oldProgress + "%", task.getProgress() + "%", null);
        }

        log.info("Task {} status {} -> {} by {}", task.getKey(), old, target, actor.getTelegramId());
        return new StatusChange(task, old);
    }

    @Transactional
    public Task setProgress(User actor, Long taskId, int progress) {
        Task task = getById(taskId);
        requireCanUpdate(actor, task);
        requireProjectOpen(task);
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new BotException(task.getKey() + " is already completed.");
        }
        if (progress < 0 || progress > 99) {
            throw new BotException("Progress must be 0–99%. Use Complete for 100%.");
        }
        int oldProgress = task.getProgress();
        task.setProgress(progress);
        record(task, actor, ChangeType.PROGRESS, oldProgress + "%", progress + "%", null);
        log.info("Task {} progress {}% by {}", task.getKey(), progress, actor.getTelegramId());
        return task;
    }

    @Transactional(readOnly = true)
    public List<Task> findMyOpenTasks(User user) {
        return taskRepository.findByAssigneeAndStatusNotOrderByDeadlineAsc(user, TaskStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public List<Task> findProjectOpenTasks(User actor, String projectCode) {
        Project project = projectService.getByCode(projectCode);
        projectService.requireCanView(actor, project);
        return taskRepository.findByProjectAndStatusNotOrderByDeadlineAsc(project, TaskStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public long countCompleted(String projectCode) {
        return taskRepository.countByProjectAndStatus(projectService.getByCode(projectCode), TaskStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public TaskWithHistory getHistory(User actor, Long taskId) {
        Task task = getForView(actor, taskId);
        List<TaskHistory> history = new ArrayList<>(
                historyRepository.findTop20ByTaskOrderByChangedAtDescIdDesc(task));
        Collections.reverse(history);
        return new TaskWithHistory(task, history);
    }

    @Transactional(readOnly = true)
    public Task prepareEditMenu(User actor, Long taskId) {
        Task task = getById(taskId);
        requireCanUpdate(actor, task);
        requireProjectOpen(task);
        requireNotCompleted(task);
        return task;
    }

    @Transactional(readOnly = true)
    public Task prepareEdit(User actor, Long taskId, TaskField field) {
        Task task = getById(taskId);
        requireCanEdit(actor, task, field);
        requireProjectOpen(task);
        requireNotCompleted(task);
        return task;
    }

    @Transactional
    public FieldChange updateField(User actor, Long taskId, TaskField field, String raw) {
        Task task = prepareEdit(actor, taskId, field);
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new BotException("Value can't be empty.");
        }

        String oldValue;
        String newValue;

        switch (field) {
            case TITLE -> {
                requireMaxLength(value, 300, "Title");
                oldValue = task.getTitle();
                newValue = value;
                task.setTitle(value);
                record(task, actor, ChangeType.TITLE, null, null, null);
            }
            case DESCRIPTION -> {
                String description = value.equals("-") ? null : requireMaxLength(value, 2000, "Description");
                oldValue = task.getDescription();
                newValue = description;
                task.setDescription(description);
                record(task, actor, ChangeType.DESCRIPTION, null, null, null);
            }
            case DELIVERABLE -> {
                requireMaxLength(value, 500, "Deliverable");
                oldValue = task.getDeliverable();
                newValue = value;
                task.setDeliverable(value);
                record(task, actor, ChangeType.DELIVERABLE, oldValue, newValue, null);
            }
            case START_DATE -> {
                LocalDate start = DateParser.parse(value, "start date");
                if (start.isAfter(task.getDeadline())) {
                    throw new BotException("Start date can't be after the deadline ("
                            + task.getDeadline().format(DateParser.FORMAT) + ").");
                }
                oldValue = task.getStartDate().format(DateParser.FORMAT);
                newValue = start.format(DateParser.FORMAT);
                task.setStartDate(start);
                record(task, actor, ChangeType.START_DATE, oldValue, newValue, null);
            }
            case DEADLINE -> {
                LocalDate deadline = DateParser.parse(value, "deadline");
                if (deadline.isBefore(task.getStartDate())) {
                    throw new BotException("Deadline can't be before the start date ("
                            + task.getStartDate().format(DateParser.FORMAT) + ").");
                }
                if (deadline.isBefore(LocalDate.now(appProperties.timezone()))) {
                    throw new BotException("Deadline can't be in the past.");
                }
                oldValue = task.getDeadline().format(DateParser.FORMAT);
                newValue = deadline.format(DateParser.FORMAT);
                task.setDeadline(deadline);
                record(task, actor, ChangeType.DEADLINE, oldValue, newValue, null);
            }
            default -> throw new IllegalStateException("Unexpected field " + field);
        }

        log.info("Task {} field {} updated by {}", task.getKey(), field, actor.getTelegramId());
        return new FieldChange(task, oldValue, newValue);
    }

// ---------- Priority ----------

    @Transactional(readOnly = true)
    public Task prepareManage(User actor, Long taskId) {
        Task task = getById(taskId);
        projectService.requireCanManage(actor, task.getProject());
        requireProjectOpen(task);
        requireNotCompleted(task);
        return task;
    }

    @Transactional
    public FieldChange changePriority(User actor, Long taskId, TaskPriority priority) {
        Task task = prepareManage(actor, taskId);
        TaskPriority old = task.getPriority();
        if (old == priority) {
            throw new BotException(task.getKey() + " already has " + priority.getLabel() + " priority.");
        }
        task.setPriority(priority);
        record(task, actor, ChangeType.PRIORITY, old.getLabel(), priority.getLabel(), null);
        log.info("Task {} priority {} -> {} by {}", task.getKey(), old, priority, actor.getTelegramId());
        return new FieldChange(task, old.getLabel(), priority.getLabel());
    }


    @Transactional(readOnly = true)
    public ReassignOptions getReassignOptions(User actor, Long taskId) {
        Task task = prepareManage(actor, taskId);
        Project project = task.getProject();

        List<User> candidates = new ArrayList<>();
        candidates.add(project.getProjectManager());
        candidates.addAll(projectService.getTeam(actor, project.getCode()).members());
        candidates.removeIf(u -> !u.isActive() || u.getId().equals(task.getAssignee().getId()));

        if (candidates.isEmpty()) {
            throw new BotException("Nobody else is in the " + project.getCode()
                    + " team. Add someone first: /addmember " + project.getCode() + " @username");
        }
        return new ReassignOptions(task, candidates);
    }

    @Transactional
    public Reassignment reassign(User actor, Long taskId, Long newAssigneeId) {
        Task task = prepareManage(actor, taskId);
        User newAssignee = userService.getById(newAssigneeId);
        User old = task.getAssignee();

        if (old.getId().equals(newAssignee.getId())) {
            throw new BotException(newAssignee.getShortName() + " is already responsible for " + task.getKey() + ".");
        }
        if (!newAssignee.isActive() || !projectService.isOnTeam(task.getProject(), newAssignee)) {
            throw new BotException(newAssignee.getShortName() + " is not in the "
                    + task.getProject().getCode() + " team.");
        }

        task.setAssignee(newAssignee);
        record(task, actor, ChangeType.ASSIGNEE, old.getShortName(), newAssignee.getShortName(), null);
        log.info("Task {} reassigned {} -> {} by {}", task.getKey(),
                old.getTelegramId(), newAssignee.getTelegramId(), actor.getTelegramId());
        return new Reassignment(task, old);
    }


    private Task getById(Long taskId) {
        return taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new BotException("Task not found."));
    }

    private void requireCanUpdate(User actor, Task task) {
        boolean allowed = task.getAssignee().getId().equals(actor.getId())
                || projectService.canManage(actor, task.getProject());
        if (!allowed) {
            throw new BotException("Only the responsible person or PM can update " + task.getKey() + ".");
        }
    }

    private void requireProjectOpen(Task task) {
        ProjectStatus ps = task.getProject().getStatus();
        if (ps == ProjectStatus.COMPLETED || ps == ProjectStatus.ARCHIVED) {
            throw new BotException("Project " + task.getProject().getCode() + " is " + ps + ". Tasks can't be changed.");
        }
    }

    private void checkTransition(User actor, Task task, TaskStatus target) {
        requireCanUpdate(actor, task);
        requireProjectOpen(task);
        if (!task.getStatus().next().contains(target)) {
            throw new BotException("Can't change " + task.getKey() + " from " + task.getStatus() + " to " + target + ".");
        }
        if (task.getStatus() == TaskStatus.COMPLETED && !projectService.canManage(actor, task.getProject())) {
            throw new BotException("Only the PM can reopen a completed task.");
        }
    }

    private Long parseKey(String raw) {
        String s = raw.trim().toUpperCase().replace("TASK-", "");
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new BotException("Invalid task id '" + raw + "'. Example: TASK-001");
        }
    }

    private void record(Task task, User actor, ChangeType type,
                        Object oldValue, Object newValue, String comment) {
        historyRepository.save(new TaskHistory(task, actor, type,
                oldValue == null ? null : oldValue.toString(),
                newValue == null ? null : newValue.toString(),
                comment));
    }


    private void requireCanEdit(User actor, Task task, TaskField field) {
        boolean isAssignee = task.getAssignee().getId().equals(actor.getId());
        boolean assigneeAllowed = field == TaskField.DESCRIPTION || field == TaskField.DELIVERABLE;
        if (projectService.canManage(actor, task.getProject()) || (isAssignee && assigneeAllowed)) {
            return;
        }
        throw new BotException(isAssignee
                ? "Only the PM can change " + field.getLabel().toLowerCase() + ". If the task is at risk, use Block with a reason."
                : "Only the PM can edit " + task.getKey() + ".");
    }

    private void requireNotCompleted(Task task) {
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new BotException(task.getKey() + " is completed. Reopen it first.");
        }
    }

    private String requireMaxLength(String value, int max, String fieldName) {
        if (value.length() > max) {
            throw new BotException(fieldName + " is too long (max " + max + " characters).");
        }
        return value;
    }


}
