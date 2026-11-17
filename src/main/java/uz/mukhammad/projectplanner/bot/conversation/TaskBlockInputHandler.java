package uz.mukhammad.projectplanner.bot.conversation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.TaskNotifier;
import uz.mukhammad.projectplanner.bot.view.TaskView;
import uz.mukhammad.projectplanner.task.StatusChange;
import uz.mukhammad.projectplanner.task.TaskService;
import uz.mukhammad.projectplanner.task.TaskStatus;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

@Component
@RequiredArgsConstructor
public class TaskBlockInputHandler implements InputHandler {

    public static final String NAME = "task_block";

    private final AccessService accessService;
    private final TaskService taskService;
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

        StatusChange change = taskService.changeStatus(actor, state.entityId(), TaskStatus.BLOCKED, message.getText());

        conversationStore.clear(new ConversationKey(message.getChatId(), message.getFrom().getId()));
        messageSender.reply(message, taskView.card(change.task()), taskView.keyboard(change.task()));
        taskNotifier.statusChanged(change, actor);
    }
}
