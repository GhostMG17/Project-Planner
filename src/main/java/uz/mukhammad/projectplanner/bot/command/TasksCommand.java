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

import java.util.List;

@Component
@RequiredArgsConstructor
public class TasksCommand implements CommandHandler {

    private final AccessService accessService;
    private final TaskService taskService;
    private final TaskView taskView;
    private final MessageSender messageSender;

    @Override
    public String command() {
        return "/tasks";
    }

    @Override
    public String description() {
        return "View project tasks: /tasks CODE";
    }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());
        String[] args = CommandArgs.parse(message, 1, "Usage: /tasks CODE");
        String code = args[0].toUpperCase();

        List<Task> tasks = taskService.findProjectOpenTasks(actor, code);
        long completed = taskService.countCompleted(code);

        if (tasks.isEmpty()) {
            messageSender.reply(message,
                    "🎉 No open tasks in " + code + ". Completed: " + completed + ".");
            return;
        }

        String title = "📋 " + code + " · open tasks (" + tasks.size() + "), completed: " + completed;
        messageSender.reply(message, taskView.list(title, tasks), taskView.openButtons(tasks));
    }
}