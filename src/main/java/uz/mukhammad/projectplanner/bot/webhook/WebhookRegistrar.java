package uz.mukhammad.projectplanner.bot.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import uz.mukhammad.projectplanner.bot.BotProperties;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "telegram.bot.mode", havingValue = "webhook")
public class WebhookRegistrar {

    public static final String PATH = "/telegram/webhook";

    private final TelegramClient telegramClient;
    private final BotProperties props;

    @EventListener(ApplicationReadyEvent.class)
    public void register() throws TelegramApiException {
        String url = props.webhookUrl().replaceAll("/+$", "") + PATH;
        telegramClient.execute(SetWebhook.builder()
                .url(url)
                .secretToken(props.webhookSecret())
                .build());
        log.info("Bot @{} registered via webhook: {}", props.username(), url);
    }
}