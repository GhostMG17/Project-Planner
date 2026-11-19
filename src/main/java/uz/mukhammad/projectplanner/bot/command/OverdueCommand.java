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
public class OverdueCommand implements CommandHandler {

    private final AccessService accessService;
    private final TaskService taskService;
    private final TaskView taskView;
    private final MessageSender messageSender;

    @Override public String command() { return "/overdue"; }
    @Override public String description() { return "View overdue tasks"; }

    @Override
    public void handle(Message message) {
        User user = accessService.requireUser(message.getFrom().getId());
        List<Task> tasks = taskService.findOverdue(user);

        if (tasks.isEmpty()) {
            messageSender.reply(message, "🎉 No overdue tasks.");
            return;
        }
        messageSender.reply(message,
                taskView.list("🚨 Overdue tasks (" + tasks.size() + ")", tasks),
                taskView.openButtons(tasks));
    }
}
