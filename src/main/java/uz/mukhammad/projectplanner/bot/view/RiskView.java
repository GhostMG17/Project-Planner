package uz.mukhammad.projectplanner.bot.view;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;
import uz.mukhammad.projectplanner.risk.entity.Risk;
import uz.mukhammad.projectplanner.risk.enums.RiskSeverity;
import uz.mukhammad.projectplanner.risk.records.RiskList;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;

@Component
public class RiskView {

    public static final String PREFIX = "rsk";

    public String list(RiskList rl) {
        Project p = rl.project();
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ ").append(p.getCode()).append(" · Risks (").append(rl.risks().size()).append(" open)\n")
                .append(p.getName()).append("\n\n");

        if (rl.risks().isEmpty()) {
            sb.append("No open risks 🎉");
            return sb.toString();
        }

        int i = 1;
        for (Risk r : rl.risks()) {
            sb.append(i++).append(". ").append(r.getSeverity().getIcon()).append(" ")
                    .append(r.getSeverity().getLabel()).append(" · owner ").append(r.getOwner().getShortName())
                    .append("\n   ").append(r.getDescription()).append("\n");
        }
        return sb.toString();
    }

    public InlineKeyboardMarkup listKeyboard(RiskList rl) {
        Project p = rl.project();
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> kb = InlineKeyboardMarkup.builder();

        InlineKeyboardRow row = new InlineKeyboardRow();
        int i = 1;
        for (Risk r : rl.risks()) {
            row.add(button("✅ Close #" + i++, "close", r.getId()));
            if (row.size() == 3) {
                kb.keyboardRow(row);
                row = new InlineKeyboardRow();
            }
        }
        if (!row.isEmpty()) {
            kb.keyboardRow(row);
        }

        if (p.getStatus() == ProjectStatus.ACTIVE || p.getStatus() == ProjectStatus.ON_HOLD) {
            kb.keyboardRow(new InlineKeyboardRow(button("➕ Add risk", "add", p.getId())));
        }
        // кнопка ведёт в обработчик ПРОЕКТОВ — callbackData может указывать на любой префикс
        kb.keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text("« Project").callbackData(ProjectView.PREFIX + ":card:" + p.getId()).build()));
        return kb.build();
    }

    /** /risks без аргументов — по всем проектам */
    public String overview(List<Risk> risks) {
        if (risks.isEmpty()) {
            return "⚠️ No open risks in your projects 🎉";
        }
        StringBuilder sb = new StringBuilder("⚠️ Open risks (" + risks.size() + ")\n");
        String currentProject = null;
        for (Risk r : risks) {
            String code = r.getProject().getCode();
            if (!code.equals(currentProject)) {
                sb.append("\n📁 ").append(code).append(" · ").append(r.getProject().getName()).append("\n");
                currentProject = code;
            }
            sb.append("  ").append(r.getSeverity().getIcon()).append(" ").append(r.getDescription())
                    .append(" (").append(r.getOwner().getShortName()).append(")\n");
        }
        sb.append("\nDetails and actions: /risks CODE");
        return sb.toString();
    }

    // ---------- Мастер ----------

    public String descriptionPrompt(Project p) {
        return "⚠️ New risk for " + p.getCode() + " · step 1/3\n\n"
                + "Describe the risk: what may happen and why.\n"
                + "Example: Client may delay approval of the requirements by 2 weeks.\n\n/cancel to abort.";
    }

    public String severityPrompt() {
        return "⚠️ New risk · step 2/3\n\nHow severe is it?";
    }

    public String ownerPrompt() {
        return "⚠️ New risk · step 3/3\n\nWho will own and monitor this risk?";
    }

    public InlineKeyboardMarkup severityKeyboard() {
        RiskSeverity[] all = RiskSeverity.values();
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(severityButton(all[0]), severityButton(all[1])))
                .keyboardRow(new InlineKeyboardRow(severityButton(all[2]), severityButton(all[3])))
                .keyboardRow(cancelRow())
                .build();
    }

    public InlineKeyboardMarkup ownerKeyboard(List<User> team, User actor) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> kb = InlineKeyboardMarkup.builder();
        for (User u : team) {
            String label = "👤 " + u.getShortName() + (u.getId().equals(actor.getId()) ? " (me)" : "");
            kb.keyboardRow(new InlineKeyboardRow(button(label, "own", u.getId())));
        }
        kb.keyboardRow(cancelRow());
        return kb.build();
    }

    public InlineKeyboardMarkup cancelKeyboard() {
        return InlineKeyboardMarkup.builder().keyboardRow(cancelRow()).build();
    }


    private InlineKeyboardRow cancelRow() {
        return new InlineKeyboardRow(button("✖ Cancel", "cancel", 0L));
    }

    private InlineKeyboardButton severityButton(RiskSeverity s) {
        return InlineKeyboardButton.builder()
                .text(s.getIcon() + " " + s.getLabel())
                .callbackData(PREFIX + ":sev:" + s.name())
                .build();
    }

    private InlineKeyboardButton button(String text, String action, Long value) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(PREFIX + ":" + action + ":" + value)
                .build();
    }
}