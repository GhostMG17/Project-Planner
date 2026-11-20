package uz.mukhammad.projectplanner.bot.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.milestone.records.MilestoneList;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;
import uz.mukhammad.projectplanner.report.records.ProjectReport;
import uz.mukhammad.projectplanner.report.records.Workload;
import uz.mukhammad.projectplanner.task.entity.Task;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ReportView {

    public static final String PREFIX = "rpt";
    private static final int MAX_ITEMS = 5;
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd-MM");

    private final TaskView taskView;

    public String projectReport(ProjectReport r) {
        Project p = r.project();
        StringBuilder sb = new StringBuilder();

        sb.append("📊 PROJECT REPORT · ").append(p.getCode()).append("\n")
                .append(p.getName()).append("\n")
                .append("Generated ").append(r.today().format(DateParser.FORMAT))
                .append(" by ").append(r.generatedBy().getShortName()).append("\n");

        // 1. Overall status
        section(sb, "1️⃣ Overall status");
        sb.append("Status: ").append(p.getStatus());
        if (p.getStatus() == ProjectStatus.ACTIVE || p.getStatus() == ProjectStatus.ON_HOLD) {
            sb.append(" · Health: ").append(r.health().label());
            if (r.health().atRisk()) {
                sb.append(" (").append(r.health().reason()).append(")");
            }
        }
        sb.append("\nPM: ").append(p.getProjectManager().getShortName()).append("\n");
        sb.append("Period: ").append(p.getStartDate().format(DateParser.FORMAT))
                .append(" → ").append(p.getPlannedEndDate().format(DateParser.FORMAT));
        boolean open = p.getStatus() == ProjectStatus.ACTIVE || p.getStatus() == ProjectStatus.ON_HOLD;
        if (open) {
            sb.append(" (").append(daysLeft(r.today(), p.getPlannedEndDate())).append(")");
        }
        sb.append("\n");
        sb.append("Progress: ").append(bar(r.progress())).append(" ").append(r.progress()).append("%\n");
        MilestoneList ml = r.milestones();
        sb.append("Milestones: ").append(ml.completed()).append("/").append(ml.milestones().size()).append(" done");
        if (ml.missed() > 0) {
            sb.append(", ").append(ml.missed()).append(" missed ❌");
        }
        sb.append("\n");

        // 2. Distribution
        section(sb, "2️⃣ Task status distribution");
        long total = r.distribution().values().stream().mapToLong(Long::longValue).sum();
        r.distribution().forEach((status, count) ->
                sb.append(taskView.statusLabel(status)).append(": ").append(count).append("\n"));
        sb.append("Total: ").append(total).append("\n");

        // 3. Completed
        section(sb, "3️⃣ Completed (last 7 days): " + r.completedLastWeek().size());
        appendTasks(sb, r.completedLastWeek(), t -> " · done");

        // 4. Outstanding
        section(sb, "4️⃣ Outstanding: " + r.open().size());
        appendTasks(sb, r.open(), t -> " · due " + t.getDeadline().format(SHORT)
                + " · " + taskView.statusLabel(t.getStatus()) + " " + t.getProgress() + "%");

        // 5. Overdue
        section(sb, "5️⃣ Overdue: " + r.overdue().size());
        appendTasks(sb, r.overdue(), t -> " · " + ChronoUnit.DAYS.between(t.getDeadline(), r.today()) + "d late");

        // 6. Upcoming
        section(sb, "6️⃣ Upcoming deadlines (7 days): " + r.upcoming().size());
        appendTasks(sb, r.upcoming(), t -> " · due " + t.getDeadline().format(SHORT));

        // 7. Risks & issues
        section(sb, "7️⃣ Risks & issues");
        sb.append("Open risks: ").append(r.risks().size()).append("\n");
        r.risks().stream().limit(MAX_ITEMS).forEach(risk ->
                sb.append("• ").append(risk.getSeverity().getIcon()).append(" ").append(risk.getDescription())
                        .append(" (").append(risk.getOwner().getShortName()).append(")\n"));
        more(sb, r.risks().size());
        sb.append("Blockers: ").append(r.blocked().size()).append("\n");
        r.blocked().stream().limit(MAX_ITEMS).forEach(t ->
                sb.append("• ").append(t.getKey()).append(" ").append(t.getTitle())
                        .append(" — ").append(t.getBlockedReason()).append("\n"));
        more(sb, r.blocked().size());

        // 8. Workload
        section(sb, "8️⃣ Workload");
        for (Workload w : r.workload()) {
            sb.append("• ").append(w.user().getShortName()).append(" — ").append(w.open()).append(" open");
            if (w.overdue() > 0) {
                sb.append(", ").append(w.overdue()).append(" overdue");
            }
            if (w.overloaded()) {
                sb.append(" ⚠️ overloaded");
            }
            sb.append("\n");
        }

        // 9. Next week
        section(sb, "9️⃣ Next week (" + r.nextWeekStart().format(SHORT) + " → " + r.nextWeekEnd().format(SHORT) + ")");
        if (r.nextWeekTasks().isEmpty() && r.nextWeekMilestones().isEmpty()) {
            sb.append("Nothing scheduled.\n");
        }
        r.nextWeekMilestones().forEach(m ->
                sb.append("🎯 ").append(m.getName()).append(" · ").append(m.getDueDate().format(SHORT)).append("\n"));
        appendTasks(sb, r.nextWeekTasks(), t -> " · due " + t.getDeadline().format(SHORT));

        return sb.toString();
    }

    /** Кнопки выбора проекта для /report без аргументов */
    public InlineKeyboardMarkup projectChooser(List<Project> projects) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> kb = InlineKeyboardMarkup.builder();
        for (Project p : projects) {
            kb.keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                    .text("📊 " + p.getCode() + " · " + p.getName())
                    .callbackData(PREFIX + ":proj:" + p.getId())
                    .build()));
        }
        return kb.build();
    }

    // ---------- helpers ----------

    private void section(StringBuilder sb, String title) {
        sb.append("\n").append(title).append("\n");
    }

    private void appendTasks(StringBuilder sb, List<Task> tasks, Function<Task, String> suffix) {
        tasks.stream().limit(MAX_ITEMS).forEach(t ->
                sb.append("• ").append(t.getPriority().getIcon()).append(" ").append(t.getKey())
                        .append(" ").append(t.getTitle())
                        .append(" — ").append(t.getAssignee().getShortName())
                        .append(suffix.apply(t)).append("\n"));
        more(sb, tasks.size());
    }

    private void more(StringBuilder sb, int size) {
        if (size > MAX_ITEMS) {
            sb.append("…and ").append(size - MAX_ITEMS).append(" more\n");
        }
    }

    private String daysLeft(LocalDate today, LocalDate end) {
        long days = ChronoUnit.DAYS.between(today, end);
        if (days > 0) {
            return days + " days left";
        }
        if (days == 0) {
            return "ends today";
        }
        return "⚠️ " + (-days) + " days past planned end";
    }

    private String bar(int progress) {
        int filled = progress / 10;
        return "▓".repeat(filled) + "░".repeat(10 - filled);
    }
}
