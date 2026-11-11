package uz.mukhammad.projectplanner.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlannerBot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        Message message = update.getMessage();

        log.info("chatId={}, threadId={}, userId={}, text={}",
                message.getChatId(),
                message.getMessageThreadId(),
                message.getFrom().getId(),
                message.getText());

        String command = extractCommand(message.getText());
        if ("/start".equals(command)) {
            reply(message, "Hi! I'm Project Planner bot. Use /help to see available commands.");
        }
    }

    private String extractCommand(String text) {
        String first = text.split("\\s+")[0];
        return first.split("@")[0];
    }

    private void reply(Message message, String text) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(message.getChatId())
                .messageThreadId(message.isTopicMessage() ? message.getMessageThreadId() : null)
                .text(text)
                .build();
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}", message.getChatId(), e);
        }
    }


}
