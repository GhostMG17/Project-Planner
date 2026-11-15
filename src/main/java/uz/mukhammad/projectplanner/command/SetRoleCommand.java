package uz.mukhammad.projectplanner.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;
import uz.mukhammad.projectplanner.user.UserService;

@Component
@RequiredArgsConstructor
public class SetRoleCommand implements CommandHandler {

    private final AccessService accessService;
    private final UserService userService;
    private final MessageSender messageSender;

    @Override
    public String command() {
        return "/setrole";
    }

    @Override
    public String description() {
        return "Change user role (admin)";
    }


    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());
        accessService.requireRole(actor, Role.ADMIN);

        String[] args = message.getText().trim().split("\\s+");
        if (args.length != 3) {
            throw new BotException(
                    "Usage: /setrole @username ADMIN|PORTFOLIO_LEAD|PROJECT_MANAGER|TEAM_MEMBER");
        }

        Role role;
        try {
            role = Role.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BotException(
                    "Invalid role. Available roles: ADMIN, PORTFOLIO_LEAD, PROJECT_MANAGER, TEAM_MEMBER");
        }

        User target = userService.changeRole(actor, args[1], role);

        messageSender.reply(message,
                "Role of @" + target.getUsername() + " changed to " + role + ".");
    }

}
