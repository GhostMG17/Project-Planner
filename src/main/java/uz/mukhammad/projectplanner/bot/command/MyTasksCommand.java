package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.view.TaskView;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.service.TaskService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MyTasksCommand implements CommandHandler {

    private final AccessService accessService;
    private final TaskService taskService;
    private final TaskView taskView;
    private final MessageSender messageSender;

    @Override public String command() { return "/mytasks"; }
    @Override public String description() { return "View personal tasks"; }

    @Override
    public void handle(Message message) {
        User user = accessService.requireUser(message.getFrom().getId());
        List<Task> tasks = taskService.findMyOpenTasks(user);

        if (tasks.isEmpty()) {
            messageSender.reply(message, "🎉 You have no open tasks.");
            return;
        }
        messageSender.reply(message,
                taskView.list("📋 My open tasks (" + tasks.size() + ")", tasks),
                taskView.openButtons(tasks));
    }
}
