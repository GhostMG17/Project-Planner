package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.view.TaskView;
import uz.mukhammad.projectplanner.common.CommandArgs;
import uz.mukhammad.projectplanner.task.Task;
import uz.mukhammad.projectplanner.task.TaskService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

@Component
@RequiredArgsConstructor
public class UpdateTaskCommand implements CommandHandler {

    private final AccessService accessService;
    private final TaskService taskService;
    private final TaskView taskView;
    private final MessageSender messageSender;

    @Override public String command() { return "/updatetask"; }
    @Override public String description() { return "Update a task: /updatetask TASK-001"; }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());
        String[] args = CommandArgs.parse(message, 1, "Usage: /updatetask TASK-001");

        Task found = taskService.getForViewByKey(actor, args[0]);
        Task t = taskService.prepareEditMenu(actor, found.getId());
        messageSender.reply(message,
                "✏️ What do you want to change in " + t.getKey() + "?", taskView.editMenuKeyboard(t));
    }
}
