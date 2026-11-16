package uz.mukhammad.projectplanner.bot.command;



import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.project.Project;
import uz.mukhammad.projectplanner.project.ProjectService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProjectsCommand implements CommandHandler {

    private final AccessService accessService;
    private final ProjectService projectService;
    private final MessageSender messageSender;

    @Override
    public String command() {
        return "/projects";
    }

    @Override
    public String description() {
        return "List projects";
    }

    @Override
    public void handle(Message message) {
        User user = accessService.requireUser(message.getFrom().getId());

        List<Project> projects = projectService.findVisibleProjects(user);

        if (projects.isEmpty()) {
            messageSender.reply(message, "No projects yet.");
            return;
        }

        StringBuilder sb = new StringBuilder(
                "Projects (" + projects.size() + "):\n\n"
        );

        for (Project project : projects) {
            sb.append(project.getCode())
                    .append(" | ")
                    .append(project.getName())
                    .append(" | PM: ")
                    .append(project.getProjectManager().getDisplayName())
                    .append(" | ")
                    .append(project.getStatus())
                    .append(" | ")
                    .append("0%\n");
        }

        messageSender.reply(message, sb.toString());
    }
}
