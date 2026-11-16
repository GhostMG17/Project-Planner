package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;

@Component
@RequiredArgsConstructor
public class CancelCommand implements CommandHandler {

    private final MessageSender messageSender;

    @Override public String command() { return "/cancel"; }
    @Override public String description() { return "Cancel current action"; }

    @Override
    public void handle(Message message) {
        messageSender.reply(message, "OK, cancelled.");
    }
}
