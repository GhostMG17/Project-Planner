package uz.mukhammad.projectplanner.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.command.CallbackHandler;
import uz.mukhammad.projectplanner.bot.command.CommandHandler;
import uz.mukhammad.projectplanner.bot.conversation.ConversationKey;
import uz.mukhammad.projectplanner.bot.conversation.ConversationState;
import uz.mukhammad.projectplanner.bot.conversation.ConversationStore;
import uz.mukhammad.projectplanner.bot.conversation.InputHandler;
import uz.mukhammad.projectplanner.common.BotException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PlannerBot implements LongPollingSingleThreadUpdateConsumer {

    private final MessageSender messageSender;
    private final BotProperties botProperties;
    private final ConversationStore conversationStore;
    private final Map<String, CommandHandler> handlers;
    private final Map<String, CallbackHandler> callbackHandlers;
    private final Map<String, InputHandler> inputHandlers;

    public PlannerBot(MessageSender messageSender,
                      BotProperties botProperties,
                      ConversationStore conversationStore,
                      List<CommandHandler> handlerList,
                      List<CallbackHandler> callbackList,
                      List<InputHandler> inputList) {
        this.messageSender = messageSender;
        this.botProperties = botProperties;
        this.conversationStore = conversationStore;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(CommandHandler::command, h -> h));
        this.callbackHandlers = callbackList.stream()
                .collect(Collectors.toMap(CallbackHandler::prefix, h -> h));
        this.inputHandlers = inputList.stream()
                .collect(Collectors.toMap(InputHandler::name, h -> h));
        log.info("Registered commands: {}, callbacks: {}, inputs: {}",
                handlers.keySet(), callbackHandlers.keySet(), inputHandlers.keySet());
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
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        Message message = update.getMessage();
        Long userId = message.getFrom() != null ? message.getFrom().getId() : null;
        log.info("chatId={}, threadId={}, userId={}, text={}",
                message.getChatId(), message.getMessageThreadId(), userId, message.getText());

        if (message.getFrom() == null) {
            return;
        }

        String text = message.getText().trim();
        ConversationKey key = new ConversationKey(message.getChatId(), message.getFrom().getId());

        if (!text.startsWith("/")) {
            handleInput(message, key);
            return;
        }

        String firstWord = text.split("\\s+")[0];
        String[] parts = firstWord.split("@", 2);
        String command = parts[0].toLowerCase();

        if (parts.length == 2 && !parts[1].equalsIgnoreCase(botProperties.username())) {
            return;
        }

        conversationStore.clear(key);

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

    private void handleInput(Message message, ConversationKey key) {
        Optional<ConversationState> state = conversationStore.get(key);
        if (state.isEmpty()) {
            return; // обычная переписка в группе — не нам
        }

        InputHandler handler = inputHandlers.get(state.get().handler());
        if (handler == null) {
            conversationStore.clear(key);
            return;
        }

        try {
            handler.handle(message, state.get());
        } catch (BotException e) {
            messageSender.reply(message, e.getMessage() + "\n\nTry again or send /cancel.");
        }
    }

    private void handleCallback(CallbackQuery cq) {
        String data = cq.getData();
        log.info("callback: userId={}, data={}", cq.getFrom().getId(), data);

        CallbackHandler handler = data == null ? null : callbackHandlers.get(data.split(":", 2)[0]);
        if (handler == null) {
            messageSender.answerCallback(cq.getId(), "Unknown action.", false);
            return;
        }

        try {
            handler.handle(cq);
            messageSender.answerCallback(cq.getId(), null, false);
        } catch (BotException e) {
            messageSender.answerCallback(cq.getId(), e.getMessage(), true);
        } catch (RuntimeException e) {
            log.error("Callback failed: data={}", data, e);
            messageSender.answerCallback(cq.getId(), "Something went wrong. Try again later.", true);
        }
    }
}