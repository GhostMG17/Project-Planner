package uz.mukhammad.projectplanner.bot.view;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.project.Project;
import uz.mukhammad.projectplanner.project.ProjectStatus;
import uz.mukhammad.projectplanner.project.ProjectTeam;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;

@Component
public class ProjectView {

    public static final String PREFIX = "prj";

    public String card(Project p) {
        User pm = p.getProjectManager();
        return "📁 " + p.getCode() + " · " + p.getName() + "\n\n"
                + "Client: " + p.getClient() + "\n"
                + "PM: " + mention(pm) + "\n"
                + "Period: " + p.getStartDate().format(DateParser.FORMAT)
                + " → " + p.getPlannedEndDate().format(DateParser.FORMAT) + "\n"
                + "Status: " + p.getStatus() + "\n"
                + "Progress: 0%";
    }

    public InlineKeyboardMarkup cardKeyboard(Project p, boolean canManage) {
        InlineKeyboardRow viewRow = new InlineKeyboardRow(button("👥 Team", "team", p));

        if (!canManage || p.getStatus() == ProjectStatus.ARCHIVED) {
            return InlineKeyboardMarkup.builder().keyboardRow(viewRow).build();
        }

        InlineKeyboardRow manageRow = new InlineKeyboardRow();
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

    private InlineKeyboardButton button(String text, String action, Project p) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(PREFIX + ":" + action + ":" + p.getId())
                .build();
    }
}
