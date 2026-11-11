package uz.mukhammad.projectplanner.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotRegistrar {
    private final TelegramBotsLongPollingApplication botsApplication;
    private final PlannerBot plannerBot;
    private final BotProperties props;

    @EventListener(ApplicationReadyEvent.class)
    public void register() throws TelegramApiException {
        botsApplication.registerBot(props.token(), plannerBot);
        log.info("Bot @{} registered and polling", props.username());
    }
}
