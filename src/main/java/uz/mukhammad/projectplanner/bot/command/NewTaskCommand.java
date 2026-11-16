package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.view.TaskView;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.project.Project;
import uz.mukhammad.projectplanner.task.CreateTaskRequest;
import uz.mukhammad.projectplanner.task.Task;
import uz.mukhammad.projectplanner.task.TaskPriority;
import uz.mukhammad.projectplanner.task.TaskService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class NewTaskCommand implements CommandHandler {

    private static final String USAGE = """
            Usage:
            /newtask CODE Title @username dd-MM-yyyy [Priority] [| Deliverable]

            Example:
            /newtask UTY Prepare financial model @john 15-11-2026 High | Financial model v1

            Priority: Low, Medium, High, Critical (default: Medium)""";

    private final AccessService accessService;
    private final TaskService taskService;
    private final TaskView taskView;
    private final MessageSender messageSender;

    @Override public String command() { return "/newtask"; }
    @Override public String description() { return "Create a task"; }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());

        String[] cmdAndArgs = message.getText().trim().split("\\s+", 2);
        if (cmdAndArgs.length < 2) {
            throw new BotException(USAGE);
        }

        String[] mainAndDeliverable = cmdAndArgs[1].split("\\|", 2);
        String deliverable = mainAndDeliverable.length == 2 ? mainAndDeliverable[1] : null;

        String[] t = mainAndDeliverable[0].trim().split("\\s+");

        int at = -1;
        for (int i = 1; i < t.length; i++) {
            if (t[i].startsWith("@")) {
                at = i;
                break;
            }
        }
        if (at < 2 || t.length < at + 2 || t.length > at + 3) {
            throw new BotException(USAGE);
        }

        CreateTaskRequest request = new CreateTaskRequest(
                t[0],
                String.join(" ", Arrays.copyOfRange(t, 1, at)),
                t[at],
                DateParser.parse(t[at + 1], "deadline"),
                t.length == at + 3 ? TaskPriority.parse(t[at + 2]) : TaskPriority.MEDIUM,
                deliverable);

        Task task = taskService.createTask(actor, request);
        String card = taskView.card(task);
        Project p = task.getProject();

        messageSender.send(p.getTelegramChatId(), p.getTelegramThreadId(), "🆕 New task\n\n" + card);

        User assignee = task.getAssignee();
        if (!assignee.getId().equals(actor.getId())) {
            messageSender.send(assignee.getTelegramId(), null,
                    "📬 New task assigned to you by " + actor.getDisplayName() + "\n\n" + card);
        }

        messageSender.reply(message, "✅ " + task.getKey() + " created.");
    }
}
