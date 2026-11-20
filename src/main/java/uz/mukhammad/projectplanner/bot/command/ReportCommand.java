package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.view.ReportView;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.report.records.ProjectReport;
import uz.mukhammad.projectplanner.report.service.ReportService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReportCommand implements CommandHandler {

    private static final Role[] ALLOWED = {Role.ADMIN, Role.PORTFOLIO_LEAD, Role.PROJECT_MANAGER};

    private final AccessService accessService;
    private final ReportService reportService;
    private final ReportView reportView;
    private final MessageSender messageSender;

    @Override public String command() { return "/report"; }
    @Override public String description() { return "Generate project report: /report or /report CODE"; }

    @Override
    public Set<Role> visibleTo() {
        return EnumSet.copyOf(Arrays.asList(ALLOWED));
    }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());
        accessService.requireRole(actor, ALLOWED);
        String[] parts = message.getText().trim().split("\\s+");

        if (parts.length == 2) {
            ProjectReport report = reportService.projectReport(actor, parts[1]);
            messageSender.replyLong(message, reportView.projectReport(report), null);
            return;
        }
        if (parts.length != 1) {
            throw new BotException("Usage: /report or /report CODE");
        }

        List<Project> projects = reportService.reportableProjects(actor);
        if (projects.isEmpty()) {
            throw new BotException("You don't manage any projects.");
        }
        if (projects.size() == 1) {
            ProjectReport report = reportService.projectReport(actor, projects.get(0).getId());
            messageSender.replyLong(message, reportView.projectReport(report), null);
            return;
        }
        messageSender.reply(message, "📊 Choose a project:", reportView.projectChooser(projects));
    }
}
