package uz.mukhammad.projectplanner.bot.view;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.enums.ProjectField;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;
import uz.mukhammad.projectplanner.project.record.ProjectTeam;
import uz.mukhammad.projectplanner.task.service.ProgressService;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;

@Component
public class ProjectView {

    private final ProgressService progressService;
    public static final String PREFIX = "prj";

    public ProjectView(ProgressService progressService) {
        this.progressService = progressService;
    }

    public String card(Project p) {
        User pm = p.getProjectManager();
        StringBuilder sb = new StringBuilder();
        sb.append("📁 ").append(p.getCode()).append(" · ").append(p.getName()).append("\n\n");
        sb.append("Client: ").append(p.getClient()).append("\n");
        sb.append("PM: ").append(mention(pm)).append("\n");
        sb.append("Period: ").append(p.getStartDate().format(DateParser.FORMAT))
                .append(" → ").append(p.getPlannedEndDate().format(DateParser.FORMAT)).append("\n");
        sb.append("Status: ").append(p.getStatus()).append("\n");
        sb.append("Progress: ").append(progressService.progressOf(p)).append("%");
        if (p.getDescription() != null) {
            sb.append("\n\n📝 ").append(p.getDescription());
        }
        if (p.getKeyDeliverables() != null) {
            sb.append("\n\n🎯 Key deliverables:\n").append(p.getKeyDeliverables());
        }
        return sb.toString();
    }

    public InlineKeyboardMarkup cardKeyboard(Project p, boolean canManage) {
        InlineKeyboardRow viewRow = new InlineKeyboardRow(button("👥 Team", "team", p));

        if (!canManage || p.getStatus() == ProjectStatus.ARCHIVED) {
            return InlineKeyboardMarkup.builder().keyboardRow(viewRow).build();
        }

        InlineKeyboardRow manageRow = new InlineKeyboardRow();
        manageRow.add(button("✏️ Edit", "edit", p));
        if (p.getStatus() == ProjectStatus.ACTIVE || p.getStatus() == ProjectStatus.ON_HOLD) {
            manageRow.add(button("✅ Close", "close", p));
        }
        manageRow.add(button("🗄 Archive", "archive", p));

        return InlineKeyboardMarkup.builder()
                .keyboardRow(viewRow)
                .keyboardRow(manageRow)
                .build();
    }

    public InlineKeyboardMarkup confirmKeyboard(Project p, String confirmAction, String confirmText) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        button(confirmText, confirmAction, p),
                        button("Cancel", "card", p)))
                .build();
    }

    public InlineKeyboardMarkup backKeyboard(Project p) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(button("« Back", "card", p)))
                .build();
    }

    public String team(ProjectTeam team) {
        Project p = team.project();
        List<User> members = team.members();

        StringBuilder sb = new StringBuilder();
        sb.append("👥 ").append(p.getCode()).append(" · ").append(p.getName()).append("\n\n");
        sb.append("PM: ").append(mention(p.getProjectManager())).append("\n\n");

        if (members.isEmpty()) {
            sb.append("Members: none yet. Add with /addmember ")
                    .append(p.getCode()).append(" @username");
        } else {
            sb.append("Members (").append(members.size()).append("):\n");
            for (User member : members) {
                sb.append("• ").append(mention(member)).append("\n");
            }
        }
        return sb.toString();
    }

    public String mention(User user) {
        return user.getUsername() != null
                ? user.getDisplayName() + " (@" + user.getUsername() + ")"
                : user.getDisplayName();
    }

    public InlineKeyboardMarkup editMenuKeyboard(Project p) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        fieldButton(ProjectField.NAME, p), fieldButton(ProjectField.CLIENT, p)))
                .keyboardRow(new InlineKeyboardRow(
                        fieldButton(ProjectField.DESCRIPTION, p), fieldButton(ProjectField.DELIVERABLES, p)))
                .keyboardRow(new InlineKeyboardRow(
                        fieldButton(ProjectField.START_DATE, p), fieldButton(ProjectField.END_DATE, p)))
                .keyboardRow(new InlineKeyboardRow(button("« Back", "card", p)))
                .build();
    }

    public InlineKeyboardMarkup cancelKeyboard(Project p) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(button("✖ Cancel", "card", p)))
                .build();
    }

    public String editPrompt(Project p, ProjectField field) {
        boolean isDate = field == ProjectField.START_DATE || field == ProjectField.END_DATE;
        return "✏️ Editing " + p.getCode() + "\n\n"
                + "Field: " + field.getLabel() + "\n"
                + "Current: " + currentValue(p, field) + "\n\n"
                + "Send the new value" + (isDate ? " in format dd-MM-yyyy" : "") + " as a message.\n"
                + "/cancel to abort.";
    }

    private String currentValue(Project p, ProjectField field) {
        String value = switch (field) {
            case NAME -> p.getName();
            case CLIENT -> p.getClient();
            case DESCRIPTION -> p.getDescription();
            case DELIVERABLES -> p.getKeyDeliverables();
            case START_DATE -> p.getStartDate().format(DateParser.FORMAT);
            case END_DATE -> p.getPlannedEndDate().format(DateParser.FORMAT);
        };
        return value == null ? "—" : value;
    }

    private InlineKeyboardButton fieldButton(ProjectField field, Project p) {
        return button(field.getLabel(), "edit_" + field.getKey(), p);
    }

    private InlineKeyboardButton button(String text, String action, Project p) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(PREFIX + ":" + action + ":" + p.getId())
                .build();
    }
}
