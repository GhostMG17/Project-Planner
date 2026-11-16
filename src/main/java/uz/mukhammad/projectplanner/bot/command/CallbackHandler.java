package uz.mukhammad.projectplanner.bot.command;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackHandler {

    String prefix();

    void handle(CallbackQuery callbackQuery);
}
