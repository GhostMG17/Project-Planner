package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.common.CommandArgs;
import uz.mukhammad.projectplanner.project.Project;
import uz.mukhammad.projectplanner.project.ProjectService;
import uz.mukhammad.projectplanner.project.ProjectTeam;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TeamCommand implements CommandHandler {

    private final AccessService accessService;
    private final ProjectService projectService;
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
        Project p = team.project();
        List<User> members = team.members();

        StringBuilder sb = new StringBuilder();
        sb.append("👥 ").append(p.getCode()).append(" · ").append(p.getName()).append("\n\n");
        sb.append("PM: ").append(formatUser(p.getProjectManager())).append("\n\n");

        if (members.isEmpty()) {
            sb.append("Members: none yet. Add with /addmember ")
                    .append(p.getCode()).append(" @username");
        } else {
            sb.append("Members (").append(members.size()).append("):\n");
            for (User member : members) {
                sb.append("• ").append(formatUser(member)).append("\n");
            }
        }

        messageSender.reply(message, sb.toString());
    }

    private String formatUser(User user) {
        return user.getUsername() != null
                ? user.getDisplayName() + " (@" + user.getUsername() + ")"
                : user.getDisplayName();
    }
}
