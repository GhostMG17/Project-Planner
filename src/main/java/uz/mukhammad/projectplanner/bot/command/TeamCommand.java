package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.view.ProjectView;
import uz.mukhammad.projectplanner.common.CommandArgs;
import uz.mukhammad.projectplanner.project.ProjectService;
import uz.mukhammad.projectplanner.project.ProjectTeam;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

@Component
@RequiredArgsConstructor
public class TeamCommand implements CommandHandler {

    private final AccessService accessService;
    private final ProjectService projectService;
    private final ProjectView projectView;
    private final MessageSender messageSender;

    @Override
    public String command() {
        return "/team";
    }

    @Override
    public String description() {
        return "Show project team: /team CODE";
    }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());
        String[] args = CommandArgs.parse(message, 1, "Usage: /team CODE");

        ProjectTeam team = projectService.getTeam(actor, args[0]);
        messageSender.reply(message, projectView.team(team));
    }
}