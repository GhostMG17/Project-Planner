package uz.mukhammad.projectplanner.bot.view;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.milestone.entity.Milestone;
import uz.mukhammad.projectplanner.milestone.enums.MilestoneStatus;
import uz.mukhammad.projectplanner.milestone.records.MilestoneList;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MilestoneView {

    public static final String PREFIX = "ms";
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd-MM");

    public String list(MilestoneList ml) {
        Project p = ml.project();
        List<Milestone> all = ml.milestones();
        StringBuilder sb = new StringBuilder();
        sb.append("🎯 ").append(p.getCode()).append(" · Milestones (")
                .append(ml.completed()).append("/").append(all.size()).append(" done");
        if (ml.missed() > 0) {
            sb.append(", ").append(ml.missed()).append(" missed");
        }
        sb.append(")\n").append(p.getName()).append("\n\n");

        if (all.isEmpty()) {
            sb.append("No milestones yet.");
            return sb.toString();
        }

        int i = 1;
        for (Milestone m : all) {
            String icon = m.getStatus() == MilestoneStatus.COMPLETED ? "✅"
                    : m.isMissed(ml.today()) ? "❌" : "⏳";
            sb.append(i++).append(". ").append(icon).append(" ").append(m.getName())
                    .append(" · ").append(m.getDueDate().format(DateParser.FORMAT));
            if (m.isMissed(ml.today())) {
                sb.append(" (missed)");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public InlineKeyboardMarkup listKeyboard(MilestoneList ml) {
        Project p = ml.project();
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> kb = InlineKeyboardMarkup.builder();
        boolean open = p.getStatus() == ProjectStatus.ACTIVE || p.getStatus() == ProjectStatus.ON_HOLD;

        if (open) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            int i = 1;
            for (Milestone m : ml.milestones()) {
                String label = (m.getStatus() == MilestoneStatus.COMPLETED ? "↩️ #" : "✅ #") + i++;
                row.add(button(label, "toggle", m.getId()));
                if (row.size() == 4) {
                    kb.keyboardRow(row);
                    row = new InlineKeyboardRow();
                }
            }
            if (!row.isEmpty()) {
                kb.keyboardRow(row);
            }
            kb.keyboardRow(new InlineKeyboardRow(button("➕ Add milestone", "add", p.getId())));
        }

        kb.keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text("« Project").callbackData(ProjectView.PREFIX + ":card:" + p.getId()).build()));
        return kb.build();
    }

    public String namePrompt(Project p) {
        return "🎯 New milestone for " + p.getCode() + " · step 1/2\n\n"
                + "Send the milestone name.\nExample: Financial model approved by client\n\n/cancel to abort.";
    }

    public String datePrompt(Project p) {
        return "🎯 New milestone · step 2/2\n\n"
                + "Choose a date or send it in format dd-MM-yyyy.\n"
                + "Project period: " + p.getStartDate().format(DateParser.FORMAT)
                + " → " + p.getPlannedEndDate().format(DateParser.FORMAT);
    }

    public InlineKeyboardMarkup dateKeyboard(Project p, LocalDate today) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> kb = InlineKeyboardMarkup.builder();
        kb.keyboardRow(new InlineKeyboardRow(
                dateButton("In 1 week", today.plusWeeks(1)),
                dateButton("In 2 weeks", today.plusWeeks(2))));
        kb.keyboardRow(new InlineKeyboardRow(
                dateButton("In 1 month", today.plusMonths(1)),
                dateButton("Project end", p.getPlannedEndDate())));
        kb.keyboardRow(cancelRow());
        return kb.build();
    }

    public InlineKeyboardMarkup cancelKeyboard() {
        return InlineKeyboardMarkup.builder().keyboardRow(cancelRow()).build();
    }

    private InlineKeyboardRow cancelRow() {
        return new InlineKeyboardRow(button("✖ Cancel", "cancel", 0L));
    }

    private InlineKeyboardButton dateButton(String label, LocalDate date) {
        return InlineKeyboardButton.builder()
                .text(label + " · " + date.format(SHORT))
                .callbackData(PREFIX + ":date:" + date)
                .build();
    }

    private InlineKeyboardButton button(String text, String action, Long value) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(PREFIX + ":" + action + ":" + value)
                .build();
    }
}