package uz.mukhammad.projectplanner.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.TopicService;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateTopicCommand implements CommandHandler {

    private final AccessService accessService;
    private final TopicService topicService;
    private final MessageSender messageSender;

    @Override
    public String command() {
        return "/createtopic";
    }

    @Override
    public String description() {
        return "Create a test topic (admin, temporary)";
    }

    @Override
    public void handle(Message message) {
        User user = accessService.requireUser(message.getFrom().getId());
        accessService.requireRole(user, Role.ADMIN);

        if (!Boolean.TRUE.equals(message.getChat().getIsForum())) {
            throw new BotException("This command works only in a group with Topics enabled.");
        }

        String[] parts = message.getText().split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            throw new BotException("Usage: /createtopic <name>");
        }
        String name = parts[1].trim();

        try {
            Integer threadId = topicService.createTopic(message.getChatId(), name);
            messageSender.send(message.getChatId(), threadId,
                    "Topic \"" + name + "\" is ready. Project updates will appear here.");
            messageSender.reply(message, "Topic created: " + name);
        } catch (TelegramApiException e) {
            log.error("Failed to create topic '{}' in chat {}", name, message.getChatId(), e);
            throw new BotException("Could not create topic. Check that I'm an admin with 'Manage Topics' permission.");
        }
    }
}
