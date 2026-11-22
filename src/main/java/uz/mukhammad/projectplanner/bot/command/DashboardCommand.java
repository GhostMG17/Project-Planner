package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.common.CommandArgs;
import uz.mukhammad.projectplanner.notification.service.DashboardPublisher;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DashboardCommand implements CommandHandler {

    private static final Role[] ALLOWED = {Role.ADMIN};

    private final AccessService accessService;
    private final DashboardPublisher dashboardPublisher;
    private final MessageSender messageSender;

    @Override public String command() { return "/dashboard"; }
    @Override public String description() { return "Publish dashboard now: /dashboard daily|weekly (admin)"; }

    @Override
    public Set<Role> visibleTo() {
        return EnumSet.copyOf(Arrays.asList(ALLOWED));
    }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());
        accessService.requireRole(actor, ALLOWED);
        String[] args = CommandArgs.parse(message, 1, "Usage: /dashboard daily | /dashboard weekly");

        switch (args[0].toLowerCase()) {
            case "daily" -> dashboardPublisher.publishDaily(true);
            case "weekly" -> dashboardPublisher.publishWeekly(true);
            default -> throw new BotException("Usage: /dashboard daily | /dashboard weekly");
        }
        messageSender.reply(message, "✅ Published to Portfolio Dashboard.");
    }
}
