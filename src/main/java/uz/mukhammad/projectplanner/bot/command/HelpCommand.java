package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HelpCommand implements CommandHandler {

    private final List<CommandHandler> handlers;
    private final MessageSender messageSender;
    private final AccessService accessService;

    @Override public String command() { return "/help"; }
    @Override public String description() { return "Show available commands"; }

    @Override
    public void handle(Message message) {
        Optional<User> user = accessService.findUser(message.getFrom().getId());

        if (user.isEmpty()) {
            messageSender.reply(message, """
                👋 Welcome to Project Planner.

                Open a private chat with me and press /start to register.
                After that, /help will show the commands available to you.""");
            return;
        }

        Role role = user.get().getRole();
        StringBuilder sb = new StringBuilder("Available commands (" + role + "):\n\n");
        handlers.stream()
                .filter(h -> h.visibleTo().contains(role))
                .sorted(Comparator.comparing(CommandHandler::command))
                .forEach(h -> sb.append(h.command()).append(" — ").append(h.description()).append("\n"));

        sb.append("\nTip: most actions are available as buttons on project and task cards.");
        messageSender.reply(message, sb.toString());
    }

}
