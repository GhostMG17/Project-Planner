package uz.mukhammad.projectplanner.bot.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.mukhammad.projectplanner.dashboard.records.DailyOverview;
import uz.mukhammad.projectplanner.dashboard.records.WeeklyReport;
import uz.mukhammad.projectplanner.milestone.records.MilestoneList;
import uz.mukhammad.projectplanner.report.records.PortfolioReport;
import uz.mukhammad.projectplanner.report.records.ProjectSummary;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DashboardView {

    private static final int MAX_ITEMS = 5;
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd-MM");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE dd-MM-yyyy", Locale.ENGLISH);

    public String daily(DailyOverview d) {
        StringBuilder sb = new StringBuilder();
        sb.append("📅 DAILY OVERVIEW · ").append(d.today().format(DAY)).append("\n\n");

        if (d.activeProjects() == 0) {
            return sb.append("No active projects.").toString();
        }

        sb.append("📁 Active projects: ").append(d.activeProjects()).append("\n");
        sb.append("📋 Open tasks: ").append(d.openTasks()).append("\n");
        sb.append("✅ Completed yesterday: ").append(d.completedYesterday()).append("\n");
        sb.append("🚨 Overdue: ").append(d.overdue()).append("\n");
        sb.append("⛔ Blocked: ").append(d.blocked()).append("\n");

        sb.append("\n⏰ Upcoming deadlines (3 days): ").append(d.upcoming().size()).append("\n");
        d.upcoming().stream().limit(MAX_ITEMS).forEach(t ->
                sb.append("• ").append(t.getProject().getCode()).append(" · ").append(t.getKey())
                        .append(" ").append(t.getTitle())
                        .append(" — ").append(t.getAssignee().getShortName())
                        .append(" · ").append(t.getDeadline().format(SHORT)).append("\n"));
        more(sb, d.upcoming().size());

        if (!d.atRisk().isEmpty()) {
            sb.append("\n⚠️ Needs attention: ");
            sb.append(d.atRisk().stream().map(s -> s.project().getCode()).collect(Collectors.joining(", ")));
            sb.append("\n");
        }

        sb.append("\nDetails: /portfolio");
        return sb.toString();
    }

    public String weekly(WeeklyReport w) {
        PortfolioReport p = w.portfolio();
        StringBuilder sb = new StringBuilder();
        sb.append("📊 WEEKLY PORTFOLIO REPORT\n")
                .append("Week ").append(w.weekStart().format(SHORT)).append(" → ").append(w.today().format(SHORT))
                .append("\n");

        if (p.projects().isEmpty()) {
            return sb.append("\nNo active projects.").toString();
        }

        // 1. Project-by-project progress + milestones
        section(sb, "1️⃣ Projects & milestones");
        for (ProjectSummary s : p.projects()) {
            MilestoneList ml = w.milestones().get(s.project());
            sb.append(s.health().atRisk() ? "🔴 " : "🟢 ").append(s.project().getCode())
                    .append(" · ").append(s.progress()).append("%")
                    .append(" · ").append(s.health().atRisk() ? "At risk" : "On track")
                    .append(" · ").append(s.overdue()).append(" overdue")
                    .append(" · milestones ").append(ml.completed()).append("/").append(ml.milestones().size());
            if (ml.missed() > 0) {
                sb.append(" (").append(ml.missed()).append(" missed ❌)");
            }
            sb.append("\n");
        }

        // 2. Work completed
        section(sb, "2️⃣ Completed this week: " + w.completedThisWeek().size());
        w.completedThisWeek().stream().limit(MAX_ITEMS).forEach(t ->
                sb.append("• ").append(t.getProject().getCode()).append(" · ").append(t.getKey())
                        .append(" ").append(t.getTitle())
                        .append(" — ").append(t.getAssignee().getShortName()).append("\n"));
        more(sb, w.completedThisWeek().size());

        // 3. Next week
        section(sb, "3️⃣ Planned next week (" + w.nextWeekStart().format(SHORT)
                + " → " + w.nextWeekEnd().format(SHORT) + ")");
        if (w.nextWeekTasks().isEmpty() && w.nextWeekMilestones().isEmpty()) {
            sb.append("Nothing scheduled.\n");
        }
        w.nextWeekMilestones().forEach(m ->
                sb.append("🎯 ").append(m.getName()).append(" · ").append(m.getDueDate().format(SHORT)).append("\n"));
        w.nextWeekTasks().stream().limit(MAX_ITEMS).forEach(t ->
                sb.append("• ").append(t.getProject().getCode()).append(" · ").append(t.getKey())
                        .append(" ").append(t.getTitle())
                        .append(" — ").append(t.getAssignee().getShortName())
                        .append(" · ").append(t.getDeadline().format(SHORT)).append("\n"));
        more(sb, w.nextWeekTasks().size());

        // 4. Risks & blockers
        section(sb, "4️⃣ Key risks & blockers");
        sb.append("Risks (High/Critical): ").append(p.keyRisks().size()).append("\n");
        p.keyRisks().stream().limit(MAX_ITEMS).forEach(r ->
                sb.append("• ").append(r.getSeverity().getIcon()).append(" ").append(r.getProject().getCode())
                        .append(": ").append(r.getDescription()).append("\n"));
        more(sb, p.keyRisks().size());
        sb.append("Blockers: ").append(w.blocked().size()).append("\n");
        w.blocked().stream().limit(MAX_ITEMS).forEach(t ->
                sb.append("• ").append(t.getProject().getCode()).append(" · ").append(t.getKey())
                        .append(" — ").append(t.getBlockedReason()).append("\n"));
        more(sb, w.blocked().size());

        // 5. Attention
        List<ProjectSummary> attention = p.projects().stream().filter(ProjectSummary::needsAttention).toList();
        section(sb, "5️⃣ Requires management attention: " + attention.size());
        if (attention.isEmpty()) {
            sb.append("All projects on track 🎉\n");
        }
        for (ProjectSummary s : attention) {
            List<String> reasons = new ArrayList<>();
            if (s.health().atRisk()) {
                reasons.add(s.health().reason());
            }
            if (s.missedMilestones() > 0) {
                reasons.add(s.missedMilestones() + " missed milestone(s)");
            }
            if (s.pastEnd()) {
                reasons.add("past planned end");
            }
            sb.append("• ").append(s.project().getCode()).append(" — ").append(String.join("; ", reasons)).append("\n");
        }

        sb.append("\nFull details: /portfolio · per project: /report CODE");
        return sb.toString();
    }

    private void section(StringBuilder sb, String title) {
        sb.append("\n").append(title).append("\n");
    }

    private void more(StringBuilder sb, int size) {
        if (size > MAX_ITEMS) {
            sb.append("…and ").append(size - MAX_ITEMS).append(" more\n");
        }
    }
}
