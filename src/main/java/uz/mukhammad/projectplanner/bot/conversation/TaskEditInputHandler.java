package uz.mukhammad.projectplanner.bot.conversation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.TaskNotifier;
import uz.mukhammad.projectplanner.bot.view.TaskView;
import uz.mukhammad.projectplanner.task.record.FieldChange;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.enums.TaskField;
import uz.mukhammad.projectplanner.task.service.TaskService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

@Component
@RequiredArgsConstructor
public class TaskEditInputHandler implements InputHandler {

    public static final String NAME = "task_edit";

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
        TaskField field = TaskField.valueOf(state.step());

        FieldChange change = taskService.updateField(actor, state.entityId(), field, message.getText());

        conversationStore.clear(new ConversationKey(message.getChatId(), message.getFrom().getId()));
        Task t = change.task();
        messageSender.reply(message, "✅ " + field.getLabel() + " updated.\n\n" + taskView.card(t),
                taskView.keyboard(t));

        if (field == TaskField.DEADLINE || field == TaskField.DELIVERABLE) {
            taskNotifier.fieldChanged(change, field.getLabel(), actor);
        }
    }
}