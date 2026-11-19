package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;
import uz.mukhammad.projectplanner.user.UserService;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UsersCommand implements CommandHandler {

    private final AccessService accessService;
    private final UserService userService;
    private final MessageSender messageSender;

    @Override public String command() { return "/users"; }
    @Override public String description() { return "List registered users (admin)"; }

    @Override
    public Set<Role> visibleTo() {
        return EnumSet.of(Role.ADMIN, Role.PORTFOLIO_LEAD);
    }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());
        accessService.requireRole(actor, Role.ADMIN, Role.PORTFOLIO_LEAD);

        List<User> users = userService.findAllActive();
        StringBuilder sb = new StringBuilder("Registered users (" + users.size() + "):\n\n");
        for (User u : users) {
            sb.append("• ").append(u.getDisplayName());
            if (u.getUsername() != null) {
                sb.append(" (@").append(u.getUsername()).append(")");
            }
            sb.append(" — ").append(u.getRole()).append("\n");
        }
        messageSender.reply(message, sb.toString());
    }
}