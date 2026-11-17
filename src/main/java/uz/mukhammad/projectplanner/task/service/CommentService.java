package uz.mukhammad.projectplanner.task.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.entity.TaskComment;
import uz.mukhammad.projectplanner.task.record.NewComment;
import uz.mukhammad.projectplanner.task.record.TaskComments;
import uz.mukhammad.projectplanner.task.repository.TaskCommentRepository;
import uz.mukhammad.projectplanner.user.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final int MAX_BODY = 2000;

    private final TaskCommentRepository commentRepository;
    private final TaskService taskService;

    @Transactional(readOnly = true)
    public Task prepareComment(User actor, Long taskId) {
        Task task = taskService.getForView(actor, taskId);
        if (task.getProject().getStatus() == ProjectStatus.ARCHIVED) {
            throw new BotException("Project " + task.getProject().getCode() + " is archived.");
        }
        return task;
    }

    @Transactional
    public TaskComment addComment(User actor, Long taskId, NewComment c) {
        Task task = prepareComment(actor, taskId);

        String body = c.body() == null || c.body().isBlank() ? null : c.body().trim();
        if (body == null && c.fileId() == null) {
            throw new BotException("Send text, a document or a photo.");
        }
        if (body != null && body.length() > MAX_BODY) {
            throw new BotException("Comment is too long (max " + MAX_BODY + " characters).");
        }
        String fileName = c.fileName() != null && c.fileName().length() > 255
                ? c.fileName().substring(0, 255)
                : c.fileName();

        TaskComment comment = commentRepository.save(
                new TaskComment(task, actor, body, c.fileId(), c.fileType(), fileName));
        log.info("Comment added to {} by {}, file={}", task.getKey(), actor.getTelegramId(), c.fileId() != null);
        return comment;
    }

    @Transactional(readOnly = true)
    public TaskComments getComments(User actor, Long taskId) {
        Task task = taskService.getForView(actor, taskId);
        List<TaskComment> last = new ArrayList<>(commentRepository.findTop10ByTaskOrderByCreatedAtDescIdDesc(task));
        Collections.reverse(last);
        return new TaskComments(task, last, commentRepository.countByTask(task));
    }

    @Transactional(readOnly = true)
    public TaskComment getFile(User actor, Long commentId) {
        TaskComment comment = commentRepository.findWithTaskById(commentId)
                .orElseThrow(() -> new BotException("Comment not found."));
        taskService.getForView(actor, comment.getTask().getId()); // проверка доступа
        if (!comment.hasFile()) {
            throw new BotException("This comment has no attachment.");
        }
        return comment;
    }
}
