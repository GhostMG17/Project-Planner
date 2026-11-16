package uz.mukhammad.projectplanner.bot.command;

import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface CommandHandler {
    /** Команда без слеша-суффикса, например "/start" */
    String command();

    /** Короткое описание для /help */
    String description();

    void handle(Message message);
}
