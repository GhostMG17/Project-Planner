package uz.mukhammad.projectplanner.bot.view;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.project.ProjectSummaryService;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.enums.ProjectField;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;
import uz.mukhammad.projectplanner.project.record.ProjectSummary;
import uz.mukhammad.projectplanner.project.record.ProjectTeam;
import uz.mukhammad.projectplanner.risk.records.ProjectHealth;
import uz.mukhammad.projectplanner.risk.service.ProjectHealthService;
import uz.mukhammad.projectplanner.task.service.ProgressService;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;

@Component
public class ProjectView {
    public static final String PREFIX = "prj";
    private final ProgressService progressService;
    private final ProjectHealthService projectHealthService;
    private final ProjectSummaryService projectSummaryService;

    public ProjectView(ProgressService progressService,
                       ProjectHealthService projectHealthService,
                       ProjectSummaryService projectSummaryService) {
        this.progressService = progressService;
        this.projectHealthService = projectHealthService;
        this.projectSummaryService = projectSummaryService;
    }

    public String card(Project p) {
        User pm = p.getProjectManager();
        ProjectSummary s = projectSummaryService.summarize(p);
        boolean open = p.getStatus() == ProjectStatus.ACTIVE || p.getStatus() == ProjectStatus.ON_HOLD;

        StringBuilder sb = new StringBuilder();
        sb.append("📁 ").append(p.getCode()).append(" · ").append(p.getName()).append("\n\n");
        sb.append("Client: ").append(p.getClient()).append("\n");
        sb.append("PM: ").append(mention(pm)).append("\n");
        sb.append("Team: ").append(s.members()).append(s.members() == 1 ? " member" : " members").append("\n");
        sb.append("Period: ").append(p.getStartDate().format(DateParser.FORMAT))
                .append(" → ").append(p.getPlannedEndDate().format(DateParser.FORMAT)).append("\n");
        sb.append("Status: ").append(p.getStatus()).append("\n");
        sb.append("Progress: ").append(progressService.progressOf(p)).append("%");
        if (open) {
            ProjectHealth h = projectHealthService.assess(p);
            sb.append("\nHealth: ").append(h.label());
            if (h.atRisk()) {
                sb.append(" (").append(h.reason()).append(")");
            }
        }
        sb.append("\nMilestones: ").append(s.milestonesDone()).append("/").append(s.milestonesTotal()).append(" done");
        sb.append("\nOpen risks: ");
        if (s.openRisks() == 0) {
            sb.append("none");
        } else {
            sb.append(s.openRisks());
            if (s.highRisks() > 0) {
                sb.append(" (High/Critical: ").append(s.highRisks()).append(")");
            }
        }

        if (p.getDescription() != null) {
            sb.append("\n\n📝 ").append(p.getDescription());
        }
        if (p.getKeyDeliverables() != null) {
            sb.append("\n\n🎯 Key deliverables:\n").append(p.getKeyDeliverables());
        } else if (open) {
            sb.append("\n\n🎯 Key deliverables: not set (Edit → Key deliverables)");
        }
        return sb.toString();
    }

    public InlineKeyboardMarkup cardKeyboard(Project p, boolean canManage) {
        InlineKeyboardRow viewRow = new InlineKeyboardRow(
                button("👥 Team", "team", p),
                external("⚠️ Risks", RiskView.PREFIX + ":list:" + p.getId()),
                external("🎯 Milestones", MilestoneView.PREFIX + ":list:" + p.getId()));

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> kb = InlineKeyboardMarkup.builder()
                .keyboardRow(viewRow);

        if (!canManage || p.getStatus() == ProjectStatus.ARCHIVED) {
            return kb.build();
        }

        InlineKeyboardRow manageRow = new InlineKeyboardRow();
        manageRow.add(button("✏️ Edit", "edit", p));
        boolean open = p.getStatus() == ProjectStatus.ACTIVE || p.getStatus() == ProjectStatus.ON_HOLD;
        if (open) {
            manageRow.add(button("✅ Close", "close", p));
        }
        manageRow.add(button("🗄 Archive", "archive", p));
        kb.keyboardRow(manageRow);

        if (open) {
            kb.keyboardRow(new InlineKeyboardRow(
                    external("📊 Report", ReportView.PREFIX + ":proj:" + p.getId()),
                    button(p.isAtRiskManual() ? "✅ Clear risk flag" : "🚩 Flag at risk", "risk", p)));
        }
        return kb.build();
    }

    private InlineKeyboardButton external(String text, String callbackData) {
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
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
