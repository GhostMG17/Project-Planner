package uz.mukhammad.projectplanner.bot.command;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.user.Role;

import java.util.EnumSet;
import java.util.Set;

public interface CommandHandler {
    /** Команда без слеша-суффикса, например "/start" */
    String command();

    /** Короткое описание для /help */
    String description();

    void handle(Message message);

    default Set<Role> visibleTo() {
        return EnumSet.allOf(Role.class);
    }
}
