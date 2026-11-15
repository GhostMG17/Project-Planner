package uz.mukhammad.projectplanner.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.command.CommandHandler;
import uz.mukhammad.projectplanner.common.BotException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PlannerBot implements LongPollingSingleThreadUpdateConsumer {

    private final MessageSender messageSender;
    private final BotProperties botProperties;
    private final Map<String, CommandHandler> handlers;

    public PlannerBot(MessageSender messageSender, BotProperties botProperties,
                      List<CommandHandler> handlerList) {
        this.messageSender = messageSender;
        this.botProperties = botProperties;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(CommandHandler::command, h -> h));
        log.info("Registered commands: {}", handlers.keySet());
    }

    @Override
    public void consume(Update update) {
        try {
            handleUpdate(update);
        } catch (Exception e) {
            log.error("Unhandled error while processing update {}", update.getUpdateId(), e);
        }
    }

    private void handleUpdate(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        Message message = update.getMessage();
        Long userId = message.getFrom() != null ? message.getFrom().getId() : null;
        log.info("chatId={}, threadId={}, userId={}, text={}",
                message.getChatId(), message.getMessageThreadId(), userId, message.getText());

        String text = message.getText().trim();
        if (!text.startsWith("/") || message.getFrom() == null) {
            return;
        }

        String firstWord = text.split("\\s+")[0];
        String[] parts = firstWord.split("@", 2);
        String command = parts[0].toLowerCase();

        if (parts.length == 2 && !parts[1].equalsIgnoreCase(botProperties.username())) {
            return;
        }

        CommandHandler handler = handlers.get(command);
        if (handler == null) {
            if (message.getChat().isUserChat()) {
                messageSender.reply(message, "Unknown command. Use /help to see available commands.");
            }
            return;
        }

        try {
            handler.handle(message);
        } catch (BotException e) {
            messageSender.reply(message, e.getMessage());
        }
    }
}
