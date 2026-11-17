package uz.mukhammad.projectplanner.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import uz.mukhammad.projectplanner.task.enums.FileType;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSender {

    private final TelegramClient telegramClient;

    public void send(Long chatId, Integer threadId, String text) {
        send(chatId, threadId, text, null);
    }

    public void send(Long chatId, Integer threadId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .messageThreadId(threadId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}, thread {}", chatId, threadId, e);
        }
    }

    public void reply(Message message, String text) {
        reply(message, text, null);
    }

    public void reply(Message message, String text, InlineKeyboardMarkup keyboard) {
        Integer threadId = message.isTopicMessage() ? message.getMessageThreadId() : null;
        send(message.getChatId(), threadId, text, keyboard);
    }

    public void edit(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        EditMessageText edit = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        try {
            telegramClient.execute(edit);
        } catch (TelegramApiException e) {
            if (e.getMessage() != null && e.getMessage().contains("message is not modified")) {
                return;
            }
            log.error("Failed to edit message {} in chat {}", messageId, chatId, e);
        }
    }

    public void answerCallback(String callbackQueryId, String text, boolean alert) {
        AnswerCallbackQuery answer = AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .text(text)
                .showAlert(alert)
                .build();
        try {
            telegramClient.execute(answer);
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback {}", callbackQueryId, e);
        }
    }

    public void sendFile(Long chatId, Integer threadId, FileType type, String fileId,
                         String caption, InlineKeyboardMarkup keyboard) {
        String safeCaption = caption != null && caption.length() > 1024
                ? caption.substring(0, 1021) + "..."
                : caption;
        try {
            if (type == FileType.PHOTO) {
                telegramClient.execute(SendPhoto.builder()
                        .chatId(chatId)
                        .messageThreadId(threadId)
                        .photo(new InputFile(fileId))
                        .caption(safeCaption)
                        .replyMarkup(keyboard)
                        .build());
            } else {
                telegramClient.execute(SendDocument.builder()
                        .chatId(chatId)
                        .messageThreadId(threadId)
                        .document(new InputFile(fileId))
                        .caption(safeCaption)
                        .replyMarkup(keyboard)
                        .build());
            }
        } catch (TelegramApiException e) {
            log.error("Failed to send file to chat {}, thread {}", chatId, threadId, e);
        }
    }

}
