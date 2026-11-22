package uz.mukhammad.projectplanner.bot.command.project_commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.command.CommandHandler;
import uz.mukhammad.projectplanner.bot.view.ProjectView;
import uz.mukhammad.projectplanner.common.CommandArgs;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

@Component
@RequiredArgsConstructor
public class ProjectCommand implements CommandHandler {

    private final AccessService accessService;
    private final ProjectService projectService;
    private final ProjectView projectView;
    private final MessageSender messageSender;

    @Override public String command() { return "/project"; }
    @Override public String description() { return "Show project card: /project CODE"; }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());
        String[] args = CommandArgs.parse(message, 1, "Usage: /project CODE");

        Project project = projectService.getByCode(args[0]);
        projectService.requireCanView(actor, project);

        messageSender.reply(message, projectView.card(project),
                projectView.cardKeyboard(project, projectService.canManage(actor, project)));
    }
}
