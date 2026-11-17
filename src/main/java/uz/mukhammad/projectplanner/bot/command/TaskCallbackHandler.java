package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.TaskNotifier;
import uz.mukhammad.projectplanner.bot.conversation.ConversationKey;
import uz.mukhammad.projectplanner.bot.conversation.ConversationStore;
import uz.mukhammad.projectplanner.bot.conversation.TaskBlockInputHandler;
import uz.mukhammad.projectplanner.bot.view.TaskView;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.task.StatusChange;
import uz.mukhammad.projectplanner.task.Task;
import uz.mukhammad.projectplanner.task.TaskService;
import uz.mukhammad.projectplanner.task.TaskStatus;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

@Component
@RequiredArgsConstructor
public class TaskCallbackHandler implements CallbackHandler {

    private final AccessService accessService;
    private final TaskService taskService;
    private final TaskView taskView;
    private final MessageSender messageSender;
    private final ConversationStore conversationStore;
    private final TaskNotifier taskNotifier;

    @Override
    public String prefix() {
        return TaskView.PREFIX;
    }

    @Override
    public void handle(CallbackQuery cq) {
        String[] parts = cq.getData().split(":");
        if (parts.length != 3) {
            throw new BotException("Unknown action.");
        }
        String action = parts[1];
        Long taskId = parseId(parts[2]);

        User actor = accessService.requireUser(cq.getFrom().getId());
        Long chatId = cq.getMessage().getChatId();
        Integer messageId = cq.getMessage().getMessageId();
        ConversationKey key = new ConversationKey(chatId, cq.getFrom().getId());

        switch (action) {
            case "card" -> {
                conversationStore.clear(key);
                Task t = taskService.getForView(actor, taskId);
                messageSender.edit(chatId, messageId, taskView.card(t), taskView.keyboard(t));
            }
            case "open" -> {
                Task t = taskService.getForView(actor, taskId);
                Integer threadId = cq.getMessage() instanceof Message m && m.isTopicMessage()
                        ? m.getMessageThreadId() : null;
                messageSender.send(chatId, threadId, taskView.card(t), taskView.keyboard(t));
            }
            case "prog" -> {
                Task t = taskService.getForView(actor, taskId);
                messageSender.edit(chatId, messageId,
                        "📊 Set progress for " + t.getKey() + "\nCurrent: " + t.getProgress() + "%",
                        taskView.progressKeyboard(t));
            }
            default -> {
                if (action.startsWith("st_")) {
                    handleStatus(action.substring(3), actor, taskId, chatId, messageId, key);
                } else if (action.startsWith("pr_")) {
                    Task t = taskService.setProgress(actor, taskId, parseInt(action.substring(3)));
                    messageSender.edit(chatId, messageId, taskView.card(t), taskView.keyboard(t));
                } else {
                    throw new BotException("Unknown action.");
                }
            }
        }
    }

    private void handleStatus(String rawStatus, User actor, Long taskId,
                              Long chatId, Integer messageId, ConversationKey key) {
        TaskStatus target;
        try {
            target = TaskStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            throw new BotException("Unknown action.");
        }

        if (target == TaskStatus.BLOCKED) {
            Task t = taskService.prepareStatusChange(actor, taskId, TaskStatus.BLOCKED);
            conversationStore.start(key, TaskBlockInputHandler.NAME, null, taskId);
            messageSender.edit(chatId, messageId,
                    "⛔ Why is " + t.getKey() + " blocked?\n\nSend the reason as a message.\n/cancel to abort.",
                    taskView.cancelKeyboard(t));
            return;
        }

        StatusChange change = taskService.changeStatus(actor, taskId, target, null);
        messageSender.edit(chatId, messageId, taskView.card(change.task()), taskView.keyboard(change.task()));
        taskNotifier.statusChanged(change, actor);
    }

    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new BotException("Unknown action.");
        }
    }

    private int parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new BotException("Unknown action.");
        }
    }
}
