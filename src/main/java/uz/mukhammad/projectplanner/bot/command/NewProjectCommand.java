package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.project.record.CreateProjectRequest;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class NewProjectCommand implements CommandHandler {

    private static final String USAGE =
            "Usage:\n/newproject CODE | Name | Client | @pm | start dd-MM-yyyy | end dd-MM-yyyy\n\n"
                    + "Example:\n/newproject UTY | Railway Transformation | Ministry of Transport | @john | 01-11-2026 | 31-03-2027";

    private final AccessService accessService;
    private final ProjectService projectService;
    private final MessageSender messageSender;

    @Override public String command() { return "/newproject"; }
    @Override public String description() { return "Create a project (PM, Portfolio Lead)"; }

    @Override
    public void handle(Message message) {
        User user = accessService.requireUser(message.getFrom().getId());
        accessService.requireRole(user, Role.ADMIN, Role.PORTFOLIO_LEAD, Role.PROJECT_MANAGER);

        String[] cmdAndArgs = message.getText().trim().split("\\s+", 2);
        if (cmdAndArgs.length < 2) {
            throw new BotException(USAGE);
        }

        String[] f = cmdAndArgs[1].split("\\|");
        if (f.length != 6 || Arrays.stream(f).anyMatch(String::isBlank)) {
            throw new BotException(USAGE);
        }

        CreateProjectRequest request = new CreateProjectRequest(
                f[0].trim(), f[1].trim(), f[2].trim(), f[3].trim(),
                DateParser.parse(f[4], "start date"),
                DateParser.parse(f[5], "end date"));

        Project project = projectService.createProject(user, request);
        messageSender.reply(message,
                "✅ Project " + project.getCode() + " created. Topic is ready in the group.");
    }
}