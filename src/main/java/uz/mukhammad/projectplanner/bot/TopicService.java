package uz.mukhammad.projectplanner.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.forum.CloseForumTopic;
import org.telegram.telegrambots.meta.api.methods.forum.CreateForumTopic;
import org.telegram.telegrambots.meta.api.methods.forum.DeleteForumTopic;
import org.telegram.telegrambots.meta.api.objects.forum.ForumTopic;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicService {

    private static final int MAX_TOPIC_NAME_LENGTH = 128;

    private final TelegramClient telegramClient;

    public Integer createTopic(Long chatId, String name) throws TelegramApiException {
        String topicName = name.length() > MAX_TOPIC_NAME_LENGTH
                ? name.substring(0, MAX_TOPIC_NAME_LENGTH)
                : name;

        CreateForumTopic request = CreateForumTopic.builder()
                .chatId(chatId)
                .name(topicName)
                .build();

        ForumTopic topic = telegramClient.execute(request);
        log.info("Topic created: chatId={}, threadId={}, name={}",
                chatId, topic.getMessageThreadId(), topic.getName());
        return topic.getMessageThreadId();

    }


    public void deleteTopicQuietly(Long chatId, Integer threadId) {
        try {
            telegramClient.execute(DeleteForumTopic.builder()
                    .chatId(chatId)
                    .messageThreadId(threadId)
                    .build());
            log.info("Topic deleted: chatId={}, threadId={}", chatId, threadId);
        } catch (TelegramApiException e) {
            log.error("Failed to delete topic {} in chat {}", threadId, chatId, e);
        }
    }

    public void closeTopicQuietly(Long chatId, Integer threadId) {
        try {
            telegramClient.execute(CloseForumTopic.builder()
                    .chatId(chatId)
                    .messageThreadId(threadId)
                    .build());
            log.info("Topic closed: chatId={}, threadId={}", chatId, threadId);
        } catch (TelegramApiException e) {
            log.error("Failed to close topic {} in chat {}", threadId, chatId, e);
        }
    }


}
