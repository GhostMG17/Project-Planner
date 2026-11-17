package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.common.CommandArgs;
import uz.mukhammad.projectplanner.project.record.MemberChange;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.User;

@Component
@RequiredArgsConstructor
public class RemoveMemberCommand implements CommandHandler {

    private final AccessService accessService;
    private final ProjectService projectService;
    private final MessageSender messageSender;

    @Override
    public String command() {
        return "/removemember";
    }

    @Override
    public String description() {
        return "Remove a member from a project: /removemember CODE @username";
    }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());

        String[] args = CommandArgs.parse(
                message, 2, "Usage: /removemember CODE @username"
        );

        MemberChange change = projectService.removeMember(actor, args[0], args[1]);

        Project p = change.project();

        messageSender.send(
                p.getTelegramChatId(),
                p.getTelegramThreadId(),
                "👤 " + change.user().getDisplayName() + " left the project team."
        );

        messageSender.reply(
                message,
                "✅ @" + change.user().getUsername() + " removed from " + p.getCode() + "."
        );
    }
}

