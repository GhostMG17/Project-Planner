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
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SetPmCommand implements CommandHandler {

    private final AccessService accessService;
    private final ProjectService projectService;
    private final MessageSender messageSender;

    @Override public String command() { return "/setpm"; }
    @Override public String description() { return "Change project manager: /setpm CODE @username"; }

    @Override
    public Set<Role> visibleTo() {
        return EnumSet.of(Role.ADMIN);
    }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());
        String[] args = CommandArgs.parse(message, 2, "Usage: /setpm CODE @username");

        MemberChange change = projectService.changeProjectManager(actor, args[0], args[1]);

        Project p = change.project();
        messageSender.send(p.getTelegramChatId(), p.getTelegramThreadId(),
                "🔄 New Project Manager: " + change.user().getDisplayName() + ".");
        messageSender.reply(message,
                "✅ @" + change.user().getUsername() + " is now the PM of " + p.getCode() + ".");
    }
}
