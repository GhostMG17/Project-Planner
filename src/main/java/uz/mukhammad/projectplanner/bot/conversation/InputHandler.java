package uz.mukhammad.projectplanner.bot.conversation;

import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface InputHandler {

    String name();

    void handle(Message message, ConversationState state);
}