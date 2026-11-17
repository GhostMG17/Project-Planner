package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.task.service.ProgressService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProjectsCommand implements CommandHandler {

    private final AccessService accessService;
    private final ProgressService progressService;
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

        Map<Long, Integer> progress = progressService.progressOf(projects);

        StringBuilder sb = new StringBuilder("Projects (" + projects.size() + "):\n\n");

        for (Project project : projects) {
            sb.append(project.getCode())
                    .append(" | ").append(project.getName())
                    .append(" | PM: ").append(project.getProjectManager().getShortName())
                    .append(" | ").append(project.getStatus())
                    .append(" | ").append(progress.getOrDefault(project.getId(), 0)).append("%\n");
        }

        messageSender.reply(message, sb.toString());
    }
}