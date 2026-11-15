package uz.mukhammad.projectplanner.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HelpCommand implements CommandHandler {

    private final List<CommandHandler> handlers;
    private final MessageSender messageSender;

    @Override public String command() { return "/help"; }
    @Override public String description() { return "Show available commands"; }

    @Override
    public void handle(Message message) {
        StringBuilder sb = new StringBuilder("Available commands:\n\n");
        handlers.stream()
                .sorted(Comparator.comparing(CommandHandler::command))
                .forEach(h -> sb.append(h.command()).append(" — ").append(h.description()).append("\n"));
        messageSender.reply(message, sb.toString());
    }

}
