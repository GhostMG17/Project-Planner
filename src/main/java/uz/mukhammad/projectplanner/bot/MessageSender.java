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
import uz.mukhammad.projectplanner.export.records.ExportFile;
import uz.mukhammad.projectplanner.task.enums.FileType;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSender {

    private final TelegramClient telegramClient;
    private static final int SAFE_LIMIT = 4000;

    public boolean send(Long chatId, Integer threadId, String text) {
        return send(chatId, threadId, text, null);
    }

    public boolean send(Long chatId, Integer threadId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .messageThreadId(threadId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        try {
            telegramClient.execute(sendMessage);
            return true;
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}, thread {}", chatId, threadId, e);
            return false;
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


    public void replyLong(Message message, String text, InlineKeyboardMarkup keyboardOnLast) {
        List<String> chunks = split(text);
        for (int i = 0; i < chunks.size(); i++) {
            boolean last = i == chunks.size() - 1;
            reply(message, chunks.get(i), last ? keyboardOnLast : null);
        }
    }

    public boolean sendLong(Long chatId, Integer threadId, String text, InlineKeyboardMarkup keyboardOnLast) {
        List<String> chunks = split(text);
        boolean allSent = true;
        for (int i = 0; i < chunks.size(); i++) {
            boolean last = i == chunks.size() - 1;
            allSent &= send(chatId, threadId, chunks.get(i), last ? keyboardOnLast : null);
        }
        return allSent;
    }

    public void sendDocument(Long chatId, Integer threadId, ExportFile file, String caption) {
        try {
            telegramClient.execute(SendDocument.builder()
                    .chatId(chatId)
                    .messageThreadId(threadId)
                    .document(new InputFile(new ByteArrayInputStream(file.content()), file.fileName()))
                    .caption(caption)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send document {} to chat {}", file.fileName(), chatId, e);
        }
    }

    public void replyDocument(Message message, ExportFile file, String caption) {
        Integer threadId = message.isTopicMessage() ? message.getMessageThreadId() : null;
        sendDocument(message.getChatId(), threadId, file, caption);
    }

    private List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            if (current.length() + line.length() + 1 > SAFE_LIMIT && !current.isEmpty()) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            current.append(line).append("\n");
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

}
