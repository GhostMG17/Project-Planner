package uz.mukhammad.projectplanner.bot.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.config.AppProperties;
import uz.mukhammad.projectplanner.project.ProjectStatus;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.entity.TaskComment;
import uz.mukhammad.projectplanner.task.entity.TaskHistory;
import uz.mukhammad.projectplanner.task.enums.ChangeType;
import uz.mukhammad.projectplanner.task.enums.TaskField;
import uz.mukhammad.projectplanner.task.enums.TaskPriority;
import uz.mukhammad.projectplanner.task.enums.TaskStatus;
import uz.mukhammad.projectplanner.task.record.TaskComments;
import uz.mukhammad.projectplanner.user.User;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        if (t.getDescription() != null) {
            sb.append("\n\n📝 ").append(t.getDescription());
        }
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
        InlineKeyboardRow lastRow = new InlineKeyboardRow();
        if (current != TaskStatus.COMPLETED) {
            lastRow.add(button("✏️ Edit", "edit", t));
        }
        lastRow.add(button("💬 Comments", "cmts", t));
        lastRow.add(button("📜 History", "hist", t));
        kb.keyboardRow(lastRow);
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

    public String history(Task t, List<TaskHistory> entries) {
        DateTimeFormatter fmt = historyTimeFormat();
        StringBuilder sb = new StringBuilder();
        sb.append("📜 ").append(t.getKey()).append(" · History\n")
                .append(t.getTitle()).append("\n\n");

        for (TaskHistory e : entries) {
            sb.append(fmt.format(e.getChangedAt()))
                    .append(" · ").append(e.getChangedBy().getShortName()).append("\n");
            sb.append("   ").append(describe(e)).append("\n");
            if (e.getComment() != null && e.getChangeType() != ChangeType.CREATED) {
                sb.append("   💬 ").append(e.getComment()).append("\n");
            }
        }
        if (entries.size() == 20) {
            sb.append("\n(showing last 20 changes)");
        }
        return sb.toString();
    }

    public InlineKeyboardMarkup backKeyboard(Task t) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(button("« Back", "card", t)))
                .build();
    }

    public InlineKeyboardMarkup editMenuKeyboard(Task t) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        fieldButton(TaskField.TITLE, t), fieldButton(TaskField.DESCRIPTION, t)))
                .keyboardRow(new InlineKeyboardRow(
                        fieldButton(TaskField.DELIVERABLE, t), button("Priority", "prio", t)))
                .keyboardRow(new InlineKeyboardRow(
                        fieldButton(TaskField.START_DATE, t), fieldButton(TaskField.DEADLINE, t)))
                .keyboardRow(new InlineKeyboardRow(button("👤 Reassign", "reas", t)))
                .keyboardRow(new InlineKeyboardRow(button("« Back", "card", t)))
                .build();
    }

    public String editPrompt(Task t, TaskField field) {
        String hint = switch (field) {
            case START_DATE, DEADLINE -> " in format dd-MM-yyyy";
            case DESCRIPTION -> " (send - to clear)";
            default -> "";
        };
        return "✏️ Editing " + t.getKey() + "\n\n"
                + "Field: " + field.getLabel() + "\n"
                + "Current: " + currentValue(t, field) + "\n\n"
                + "Send the new value" + hint + " as a message.\n/cancel to abort.";
    }

    public InlineKeyboardMarkup priorityKeyboard(Task t) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (TaskPriority p : TaskPriority.values()) {
            String label = p.getIcon() + " " + p.getLabel() + (p == t.getPriority() ? " ✓" : "");
            row.add(button(label, "pri_" + p.name(), t));
        }
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(row.subList(0, 2)))
                .keyboardRow(new InlineKeyboardRow(row.subList(2, 4)))
                .keyboardRow(new InlineKeyboardRow(button("« Back", "edit", t)))
                .build();
    }

    public InlineKeyboardMarkup reassignKeyboard(Task t, List<User> candidates) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> kb = InlineKeyboardMarkup.builder();
        for (User u : candidates) {
            kb.keyboardRow(new InlineKeyboardRow(
                    button("👤 " + u.getDisplayName() + (u.getUsername() != null ? " (@" + u.getUsername() + ")" : ""),
                            "ra_" + u.getId(), t)));
        }
        kb.keyboardRow(new InlineKeyboardRow(button("« Back", "edit", t)));
        return kb.build();
    }

    public String comments(TaskComments tc) {
        DateTimeFormatter fmt = historyTimeFormat();
        Task t = tc.task();
        StringBuilder sb = new StringBuilder();
        sb.append("💬 ").append(t.getKey()).append(" · Comments (").append(tc.total()).append(")\n")
                .append(t.getTitle()).append("\n\n");

        if (tc.comments().isEmpty()) {
            sb.append("No comments yet.");
            return sb.toString();
        }

        int fileIndex = 0;
        for (TaskComment c : tc.comments()) {
            sb.append(fmt.format(c.getCreatedAt())).append(" · ").append(c.getAuthor().getShortName()).append("\n");
            if (c.getBody() != null) {
                sb.append("   ").append(c.getBody()).append("\n");
            }
            if (c.hasFile()) {
                fileIndex++;
                sb.append("   📎 [").append(fileIndex).append("] ")
                        .append(c.getFileName() != null ? c.getFileName() : c.getFileType().name().toLowerCase())
                        .append("\n");
            }
        }
        if (tc.total() > tc.comments().size()) {
            sb.append("\n(showing last ").append(tc.comments().size()).append(")");
        }
        return sb.toString();
    }

    public InlineKeyboardMarkup commentsKeyboard(TaskComments tc) {
        Task t = tc.task();
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> kb = InlineKeyboardMarkup.builder();

        InlineKeyboardRow fileRow = new InlineKeyboardRow();
        int fileIndex = 0;
        for (TaskComment c : tc.comments()) {
            if (!c.hasFile()) {
                continue;
            }
            fileIndex++;
            fileRow.add(button("📎 " + fileIndex, "f_" + c.getId(), t));
            if (fileRow.size() == 4) {
                kb.keyboardRow(fileRow);
                fileRow = new InlineKeyboardRow();
            }
        }
        if (!fileRow.isEmpty()) {
            kb.keyboardRow(fileRow);
        }

        if (t.getProject().getStatus() != ProjectStatus.ARCHIVED) {
            kb.keyboardRow(new InlineKeyboardRow(button("➕ Add comment", "cadd", t)));
        }
        kb.keyboardRow(new InlineKeyboardRow(button("« Back", "card", t)));
        return kb.build();
    }

    public String commentPrompt(Task t) {
        return "💬 New comment for " + t.getKey() + "\n\n"
                + "Send a text message, a document or a photo (you can add a caption).\n"
                + "/cancel to abort.";
    }


    private String currentValue(Task t, TaskField field) {
        String value = switch (field) {
            case TITLE -> t.getTitle();
            case DESCRIPTION -> t.getDescription();
            case DELIVERABLE -> t.getDeliverable();
            case START_DATE -> t.getStartDate().format(DateParser.FORMAT);
            case DEADLINE -> t.getDeadline().format(DateParser.FORMAT);
        };
        return value == null ? "—" : value;
    }

    private InlineKeyboardButton fieldButton(TaskField field, Task t) {
        return button(field.getLabel(), "ed_" + field.getKey(), t);
    }

    private String describe(TaskHistory e) {
        return switch (e.getChangeType()) {
            case CREATED -> "🆕 Created" + (e.getComment() != null ? " · " + e.getComment() : "");
            case STATUS -> "Status: " + statusText(e.getOldValue()) + " → " + statusText(e.getNewValue());
            case PROGRESS -> "Progress: " + e.getOldValue() + " → " + e.getNewValue();
            case ASSIGNEE -> "Responsible: " + e.getOldValue() + " → " + e.getNewValue();
            case DEADLINE -> "Deadline: " + e.getOldValue() + " → " + e.getNewValue();
            case PRIORITY -> "Priority: " + e.getOldValue() + " → " + e.getNewValue();
            case TITLE -> "Title changed";
            case DESCRIPTION -> "Description changed";
            case DELIVERABLE -> "Deliverable: " + e.getOldValue() + " → " + e.getNewValue();
            case START_DATE -> "Start date: " + e.getOldValue() + " → " + e.getNewValue();
        };
    }

    private String statusText(String raw) {
        if (raw == null) {
            return "—";
        }
        try {
            return statusLabel(TaskStatus.valueOf(raw));
        } catch (IllegalArgumentException e) {
            return raw;
        }
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

    private DateTimeFormatter historyTimeFormat() {
        return DateTimeFormatter.ofPattern("dd-MM HH:mm").withZone(appProperties.timezone());
    }
}
