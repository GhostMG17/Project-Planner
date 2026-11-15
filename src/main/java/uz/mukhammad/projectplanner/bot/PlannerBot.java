package uz.mukhammad.projectplanner.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlannerBot implements LongPollingSingleThreadUpdateConsumer {

    private final MessageSender messageSender;
    private final TopicService topicService;

    @Override
    public void consume(Update update) {
        try {
            handleUpdate(update);
        } catch (Exception e) {
            log.error("Unhandled error while processing update {}", update.getUpdateId(), e);
        }
    }

    private String extractCommand(String text) {
        String first = text.split("\\s+")[0];
        return first.split("@")[0];
    }

    private void handleCreateTopic(Message message) {
        if (!Boolean.TRUE.equals(message.getChat().getIsForum())) {
            messageSender.reply(message, "This command works only in a group with Topics enabled.");
            return;
        }

        String[] parts = message.getText().split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            messageSender.reply(message, "Usage: /createtopic <name>");
            return;
        }
        String name = parts[1].trim();

        try {
            Integer threadId = topicService.createTopic(message.getChatId(), name);
            messageSender.send(message.getChatId(), threadId,
                    "Topic \"" + name + "\" is ready. Project updates will appear here.");
            messageSender.reply(message, "Topic created: " + name);
        } catch (TelegramApiException e) {
            log.error("Failed to create topic '{}' in chat {}", name, message.getChatId(), e);
            messageSender.reply(message, "Could not create topic. Check that I'm an admin with 'Manage Topics' permission.");
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

        String command = extractCommand(message.getText());
        switch (command) {
            case "/start" -> messageSender.reply(message,
                    "Hi! I'm Project Planner bot. Use /help to see available commands.");
            case "/createtopic" -> handleCreateTopic(message);
            default -> { }
        }
    }

}
