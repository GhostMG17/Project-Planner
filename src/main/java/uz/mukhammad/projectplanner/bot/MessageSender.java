package uz.mukhammad.projectplanner.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSender {

    private final TelegramClient telegramClient;

    public void send(Long chatId, Integer threadId, String text) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .messageThreadId(threadId)
                .text(text)
                .build();
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}, thread {}", chatId, threadId, e);
        }
    }

    public void reply(Message message, String text) {
        Integer threadId = message.isTopicMessage() ? message.getMessageThreadId() : null;
        send(message.getChatId(), threadId, text);
    }
}
