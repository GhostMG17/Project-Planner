package uz.mukhammad.projectplanner.bot.conversation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.TaskNotifier;
import uz.mukhammad.projectplanner.bot.view.TaskView;
import uz.mukhammad.projectplanner.task.entity.TaskComment;
import uz.mukhammad.projectplanner.task.enums.FileType;
import uz.mukhammad.projectplanner.task.record.NewComment;
import uz.mukhammad.projectplanner.task.record.TaskComments;
import uz.mukhammad.projectplanner.task.service.CommentService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskCommentInputHandler implements InputHandler {

    public static final String NAME = "task_comment";

    private final AccessService accessService;
    private final CommentService commentService;
    private final TaskView taskView;
    private final MessageSender messageSender;
    private final ConversationStore conversationStore;
    private final TaskNotifier taskNotifier;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void handle(Message message, ConversationState state) {
        User actor = accessService.requireUser(message.getFrom().getId());

        TaskComment comment = commentService.addComment(actor, state.entityId(), extract(message));

        conversationStore.clear(new ConversationKey(message.getChatId(), message.getFrom().getId()));

        TaskComments tc = commentService.getComments(actor, state.entityId());
        messageSender.reply(message, "✅ Comment added.\n\n" + taskView.comments(tc), taskView.commentsKeyboard(tc));

        taskNotifier.commented(comment, actor);
    }

    private NewComment extract(Message m) {
        if (m.hasDocument()) {
            Document d = m.getDocument();
            return new NewComment(m.getCaption(), d.getFileId(), FileType.DOCUMENT, d.getFileName());
        }
        if (m.hasPhoto()) {
            List<PhotoSize> sizes = m.getPhoto();
            PhotoSize largest = sizes.get(sizes.size() - 1);
            return new NewComment(m.getCaption(), largest.getFileId(), FileType.PHOTO, "photo.jpg");
        }
        return new NewComment(m.getText(), null, null, null);
    }
}
