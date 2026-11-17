package uz.mukhammad.projectplanner.bot.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.config.AppProperties;
import uz.mukhammad.projectplanner.task.Task;
import uz.mukhammad.projectplanner.task.TaskStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskView {

    public static final String PREFIX = "tsk";
    private static final int[] PROGRESS_STEPS = {10, 25, 50, 75, 90};

    private final ProjectView projectView;
    private final AppProperties appProperties;

    public String card(Task t) {
        StringBuilder sb = new StringBuilder();
        sb.append("📌 ").append(t.getKey()).append(" · ")
                .append(t.getPriority().getIcon()).append(" ").append(t.getPriority().getLabel());
        if (isOverdue(t)) {
            sb.append(" · ⚠️ OVERDUE");
        }
        sb.append("\n").append(t.getTitle()).append("\n\n");
        sb.append("Project: ").append(t.getProject().getCode()).append("\n");
        sb.append("Responsible: ").append(projectView.mention(t.getAssignee())).append("\n");
        sb.append("Deadline: ").append(t.getDeadline().format(DateParser.FORMAT)).append("\n");
        sb.append("Status: ").append(statusLabel(t.getStatus())).append("\n");
        sb.append("Progress: ").append(progressBar(t.getProgress())).append(" ").append(t.getProgress()).append("%\n");
        sb.append("Deliverable: ").append(t.getDeliverable() != null ? t.getDeliverable() : "⚠️ not set");
        if (t.getStatus() == TaskStatus.BLOCKED && t.getBlockedReason() != null) {
            sb.append("\n\n⛔ Blocked: ").append(t.getBlockedReason());
        }
        return sb.toString();
    }

    public InlineKeyboardMarkup keyboard(Task t) {
        TaskStatus current = t.getStatus();
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        for (TaskStatus target : current.next()) {
            buttons.add(button(transitionLabel(current, target), "st_" + target.name(), t));
        }
        if (current != TaskStatus.COMPLETED) {
            buttons.add(button("📊 Progress", "prog", t));
        }

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> kb = InlineKeyboardMarkup.builder();
        for (int i = 0; i < buttons.size(); i += 2) {
            kb.keyboardRow(new InlineKeyboardRow(buttons.subList(i, Math.min(i + 2, buttons.size()))));
        }
        return kb.build();
    }

    public InlineKeyboardMarkup progressKeyboard(Task t) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (int step : PROGRESS_STEPS) {
            row.add(button(step + "%", "pr_" + step, t));
        }
        return InlineKeyboardMarkup.builder()
                .keyboardRow(row)
                .keyboardRow(new InlineKeyboardRow(button("« Back", "card", t)))
                .build();
    }

    public InlineKeyboardMarkup cancelKeyboard(Task t) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(button("✖ Cancel", "card", t)))
                .build();
    }

    public String list(String title, List<Task> tasks) {
        StringBuilder sb = new StringBuilder(title).append("\n\n");
        for (Task t : tasks) {
            sb.append(t.getPriority().getIcon()).append(" ").append(t.getKey())
                    .append(" · ").append(t.getTitle()).append("\n");
            sb.append("     ").append(t.getProject().getCode())
                    .append(" · due ").append(t.getDeadline().format(DateParser.FORMAT))
                    .append(isOverdue(t) ? " ⚠️" : "")
                    .append(" · ").append(statusLabel(t.getStatus()))
                    .append(" · ").append(t.getProgress()).append("%\n");
        }
        return sb.toString();
    }

    public InlineKeyboardMarkup openButtons(List<Task> tasks) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> kb = InlineKeyboardMarkup.builder();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (Task t : tasks) {
            row.add(button(t.getKey(), "open", t));
            if (row.size() == 3) {
                kb.keyboardRow(row);
                row = new InlineKeyboardRow();
            }
        }
        if (!row.isEmpty()) {
            kb.keyboardRow(row);
        }
        return kb.build();
    }

    public boolean isOverdue(Task t) {
        return t.getStatus() != TaskStatus.COMPLETED
                && t.getDeadline().isBefore(LocalDate.now(appProperties.timezone()));
    }

    public String statusLabel(TaskStatus s) {
        return switch (s) {
            case NEW -> "🆕 New";
            case IN_PROGRESS -> "🔵 In Progress";
            case ON_HOLD -> "⏸ On Hold";
            case BLOCKED -> "⛔ Blocked";
            case COMPLETED -> "✅ Completed";
        };
    }

    private String transitionLabel(TaskStatus from, TaskStatus to) {
        return switch (to) {
            case IN_PROGRESS -> switch (from) {
                case NEW -> "▶️ Start";
                case ON_HOLD -> "▶️ Resume";
                case BLOCKED -> "▶️ Unblock";
                case COMPLETED -> "↩️ Reopen";
                default -> "▶️ In Progress";
            };
            case ON_HOLD -> "⏸ Hold";
            case BLOCKED -> "⛔ Block";
            case COMPLETED -> "✅ Complete";
            case NEW -> "New";
        };
    }

    private String progressBar(int progress) {
        int filled = progress / 10;
        return "▓".repeat(filled) + "░".repeat(10 - filled);
    }

    private InlineKeyboardButton button(String text, String action, Task t) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(PREFIX + ":" + action + ":" + t.getId())
                .build();
    }
}
