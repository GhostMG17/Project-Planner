package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.TaskNotifier;
import uz.mukhammad.projectplanner.bot.conversation.NewTaskWizard;
import uz.mukhammad.projectplanner.bot.view.TaskView;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.task.record.CreateTaskRequest;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.enums.TaskPriority;
import uz.mukhammad.projectplanner.task.service.TaskService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;
import uz.mukhammad.projectplanner.user.UserService;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class NewTaskCommand implements CommandHandler {

    private static final String USAGE = """
            Tip: send /newtask without arguments for a step-by-step form.
            
            Usage:
            /newtask CODE Title @username dd-MM-yyyy [Priority] [| Deliverable]

            Example:
            /newtask UTY Prepare financial model @john 15-11-2026 High | Financial model v1

            Priority: Low, Medium, High, Critical (default: Medium)""";

    private final AccessService accessService;
    private final TaskService taskService;
    private final TaskNotifier taskNotifier;
    private final NewTaskWizard newTaskWizard;
    private final MessageSender messageSender;
    private final UserService userService;

    @Override public String command() { return "/newtask"; }
    @Override public String description() { return "Create a task"; }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());

        String[] cmdAndArgs = message.getText().trim().split("\\s+", 2);
        if (cmdAndArgs.length < 2) {
            newTaskWizard.start(message, actor);   // без аргументов — пошаговый мастер
            return;
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

        User assignee = userService.getByUsername(t[at]);

        CreateTaskRequest request = new CreateTaskRequest(
                t[0],
                String.join(" ", Arrays.copyOfRange(t, 1, at)),
                assignee.getId(),
                DateParser.parse(t[at + 1], "deadline"),
                t.length == at + 3 ? TaskPriority.parse(t[at + 2]) : TaskPriority.MEDIUM,
                deliverable);

        Task task = taskService.createTask(actor, request);
        taskNotifier.taskCreated(task, actor);
        messageSender.reply(message, "✅ " + task.getKey() + " created.");
    }
}
