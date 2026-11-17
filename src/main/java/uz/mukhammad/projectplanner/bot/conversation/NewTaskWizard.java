package uz.mukhammad.projectplanner.bot.conversation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.TaskNotifier;
import uz.mukhammad.projectplanner.bot.command.CallbackHandler;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.config.AppProperties;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.enums.TaskPriority;
import uz.mukhammad.projectplanner.task.record.CreateTaskRequest;
import uz.mukhammad.projectplanner.task.service.TaskService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;
import uz.mukhammad.projectplanner.user.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewTaskWizard implements CallbackHandler, InputHandler {

    public static final String NAME = "task_wizard";
    public static final String PREFIX = "ntw";
    private static final int TOTAL_STEPS = 6;

    enum Step { PROJECT, TITLE, ASSIGNEE, DEADLINE, PRIORITY, DELIVERABLE, CONFIRM }

    private final AccessService accessService;
    private final ProjectService projectService;
    private final UserService userService;
    private final TaskService taskService;
    private final TaskNotifier taskNotifier;
    private final ConversationStore conversationStore;
    private final MessageSender messageSender;
    private final AppProperties appProperties;

    public void start(Message message, User actor) {
        List<Project> projects = projectService.findVisibleProjects(actor).stream()
                .filter(p -> p.getStatus() == ProjectStatus.ACTIVE || p.getStatus() == ProjectStatus.ON_HOLD)
                .toList();
        if (projects.isEmpty()) {
            throw new BotException("You have no active projects to add a task to.");
        }

        ConversationKey key = new ConversationKey(message.getChatId(), message.getFrom().getId());
        conversationStore.start(key, NAME, Step.PROJECT.name(), null);

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> kb = InlineKeyboardMarkup.builder();
        for (Project p : projects) {
            kb.keyboardRow(new InlineKeyboardRow(button("📁 " + p.getCode() + " · " + p.getName(), "proj", p.getId())));
        }
        kb.keyboardRow(cancelRow());

        messageSender.reply(message, header(1) + "Choose a project:", kb.build());
    }


    @Override
    public String prefix() {
        return PREFIX;
    }

    @Override
    public void handle(CallbackQuery cq) {
        User actor = accessService.requireUser(cq.getFrom().getId());
        Long chatId = cq.getMessage().getChatId();
        Integer messageId = cq.getMessage().getMessageId();
        ConversationKey key = new ConversationKey(chatId, cq.getFrom().getId());

        ConversationState state = conversationStore.get(key)
                .filter(s -> NAME.equals(s.handler()))
                .orElseThrow(() -> new BotException(
                        "This form has expired or belongs to someone else. Start again: /newtask"));

        String[] parts = cq.getData().split(":", 3);
        if (parts.length != 3) {
            throw new BotException("Unknown action.");
        }
        String action = parts[1];
        String value = parts[2];
        Step step = Step.valueOf(state.step());
        Map<String, String> data = state.data();

        switch (action) {
            case "cancel" -> {
                conversationStore.clear(key);
                messageSender.edit(chatId, messageId, "✖ Task creation cancelled.", null);
                return;
            }
            case "proj" -> {
                requireStep(step, Step.PROJECT);
                Project p = projectService.getById(parseLong(value));
                projectService.requireCanView(actor, p);
                data.put("projectId", p.getId().toString());
                data.put("projectCode", p.getCode());
                goTo(key, Step.TITLE);
            }
            case "asg" -> {
                requireStep(step, Step.ASSIGNEE);
                User assignee = userService.getById(parseLong(value));
                data.put("assigneeId", assignee.getId().toString());
                data.put("assigneeName", assignee.getShortName());
                goTo(key, Step.DEADLINE);
            }
            case "dl" -> {
                requireStep(step, Step.DEADLINE);
                setDeadline(data, LocalDate.parse(value));
                goTo(key, Step.PRIORITY);
            }
            case "pri" -> {
                requireStep(step, Step.PRIORITY);
                data.put("priority", TaskPriority.valueOf(value).name());
                goTo(key, Step.DELIVERABLE);
            }
            case "ok" -> {
                requireStep(step, Step.CONFIRM);
                Task task = create(actor, data);
                conversationStore.clear(key);
                messageSender.edit(chatId, messageId, "✅ " + task.getKey() + " created.\n\n" + summary(data), null);
                taskNotifier.taskCreated(task, actor);
                return;
            }
            default -> throw new BotException("Unknown action.");
        }

        Step next = Step.valueOf(conversationStore.get(key).orElseThrow().step());
        messageSender.edit(chatId, messageId, prompt(next, data, actor), keyboard(next, data, actor));
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void handle(Message message, ConversationState state) {
        User actor = accessService.requireUser(message.getFrom().getId());
        ConversationKey key = new ConversationKey(message.getChatId(), message.getFrom().getId());
        Step step = Step.valueOf(state.step());
        Map<String, String> data = state.data();

        if (!message.hasText()) {
            throw new BotException("Please send text.");
        }
        String text = message.getText().trim();

        switch (step) {
            case TITLE -> {
                if (text.isEmpty() || text.length() > 300) {
                    throw new BotException("Task title must be 1–300 characters.");
                }
                data.put("title", text);
                goTo(key, Step.ASSIGNEE);
            }
            case DEADLINE -> {
                setDeadline(data, DateParser.parse(text, "deadline"));
                goTo(key, Step.PRIORITY);
            }
            case DELIVERABLE -> {
                if (text.isEmpty() || text.length() > 500) {
                    throw new BotException("Deliverable must be 1–500 characters.");
                }
                data.put("deliverable", text);
                goTo(key, Step.CONFIRM);
            }
            default -> throw new BotException("Please use the buttons above.");
        }

        Step next = Step.valueOf(conversationStore.get(key).orElseThrow().step());
        messageSender.reply(message, prompt(next, data, actor), keyboard(next, data, actor));
    }

    // ===================== Шаги =====================

    private String prompt(Step step, Map<String, String> data, User actor) {
        return switch (step) {
            case TITLE -> header(2) + "Project: " + data.get("projectCode") + "\n\nSend the task title.";
            case ASSIGNEE -> header(3) + "Task: " + data.get("title") + "\n\nWho is responsible?";
            case DEADLINE -> header(4) + "Choose a deadline or send a date in format dd-MM-yyyy.";
            case PRIORITY -> header(5) + "Choose priority:";
            case DELIVERABLE -> header(6) + "What is the expected output?\nExample: Financial model v1";
            case CONFIRM -> "🆕 New task · check and confirm\n\n" + summary(data);
            case PROJECT -> header(1) + "Choose a project:";
        };
    }

    private InlineKeyboardMarkup keyboard(Step step, Map<String, String> data, User actor) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> kb = InlineKeyboardMarkup.builder();
        switch (step) {
            case ASSIGNEE -> {
                Project p = projectService.getById(parseLong(data.get("projectId")));
                List<User> team = new ArrayList<>();
                team.add(p.getProjectManager());
                team.addAll(projectService.getTeam(actor, p.getCode()).members());
                for (User u : team) {
                    if (!u.isActive()) {
                        continue;
                    }
                    String label = "👤 " + u.getShortName() + (u.getId().equals(actor.getId()) ? " (me)" : "");
                    kb.keyboardRow(new InlineKeyboardRow(button(label, "asg", u.getId())));
                }
            }
            case DEADLINE -> {
                LocalDate today = LocalDate.now(appProperties.timezone());
                kb.keyboardRow(new InlineKeyboardRow(
                        dateButton("Tomorrow", today.plusDays(1)),
                        dateButton("In 3 days", today.plusDays(3))));
                kb.keyboardRow(new InlineKeyboardRow(
                        dateButton("In 1 week", today.plusWeeks(1)),
                        dateButton("In 2 weeks", today.plusWeeks(2))));
            }
            case PRIORITY -> {
                TaskPriority[] all = TaskPriority.values();
                kb.keyboardRow(new InlineKeyboardRow(priorityButton(all[0]), priorityButton(all[1])));
                kb.keyboardRow(new InlineKeyboardRow(priorityButton(all[2]), priorityButton(all[3])));
            }
            case CONFIRM -> kb.keyboardRow(new InlineKeyboardRow(
                    button("✅ Create", "ok", 0L), button("✖ Cancel", "cancel", 0L)));
            default -> { }
        }
        if (step != Step.CONFIRM) {
            kb.keyboardRow(cancelRow());
        }
        return kb.build();
    }


    private Task create(User actor, Map<String, String> data) {
        CreateTaskRequest request = new CreateTaskRequest(
                data.get("projectCode"),
                data.get("title"),
                parseLong(data.get("assigneeId")),
                LocalDate.parse(data.get("deadline")),
                TaskPriority.valueOf(data.get("priority")),
                data.get("deliverable"));
        return taskService.createTask(actor, request);
    }

    private String summary(Map<String, String> data) {
        TaskPriority p = TaskPriority.valueOf(data.get("priority"));
        return "Project: " + data.get("projectCode") + "\n"
                + "Title: " + data.get("title") + "\n"
                + "Responsible: " + data.get("assigneeName") + "\n"
                + "Deadline: " + LocalDate.parse(data.get("deadline")).format(DateParser.FORMAT) + "\n"
                + "Priority: " + p.getIcon() + " " + p.getLabel() + "\n"
                + "Deliverable: " + data.get("deliverable");
    }

    private void setDeadline(Map<String, String> data, LocalDate deadline) {
        if (deadline.isBefore(LocalDate.now(appProperties.timezone()))) {
            throw new BotException("Deadline can't be in the past.");
        }
        data.put("deadline", deadline.toString()); // ISO yyyy-MM-dd для хранения
    }

    private void goTo(ConversationKey key, Step next) {
        conversationStore.advance(key, next.name());
    }

    private void requireStep(Step actual, Step expected) {
        if (actual != expected) {
            throw new BotException("This button is outdated. Please use the latest message.");
        }
    }

    private String header(int step) {
        return "🆕 New task · step " + step + "/" + TOTAL_STEPS + "\n\n";
    }

    private InlineKeyboardRow cancelRow() {
        return new InlineKeyboardRow(button("✖ Cancel", "cancel", 0L));
    }

    private InlineKeyboardButton dateButton(String label, LocalDate date) {
        return InlineKeyboardButton.builder()
                .text(label + " · " + date.format(DateTimeFormatter.ofPattern("dd-MM")))
                .callbackData(PREFIX + ":dl:" + date)
                .build();
    }

    private InlineKeyboardButton priorityButton(TaskPriority p) {
        return InlineKeyboardButton.builder()
                .text(p.getIcon() + " " + p.getLabel())
                .callbackData(PREFIX + ":pri:" + p.name())
                .build();
    }

    private InlineKeyboardButton button(String text, String action, Long value) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(PREFIX + ":" + action + ":" + value)
                .build();
    }

    private Long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new BotException("Unknown action.");
        }
    }
}
