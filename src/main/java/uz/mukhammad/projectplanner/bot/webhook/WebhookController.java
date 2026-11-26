package uz.mukhammad.projectplanner.bot.webhook;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.mukhammad.projectplanner.bot.BotProperties;
import uz.mukhammad.projectplanner.bot.PlannerBot;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "telegram.bot.mode", havingValue = "webhook")
public class WebhookController {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final PlannerBot plannerBot;
    private final BotProperties props;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "tg-webhook"));

    @PostMapping(WebhookRegistrar.PATH)
    public ResponseEntity<Void> onUpdate(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secret,
            @RequestBody String body) {

        if (!secretMatches(secret)) {
            log.warn("Webhook call with invalid secret token rejected");
            return ResponseEntity.status(403).build();
        }

        Update update;
        try {
            update = MAPPER.readValue(body, Update.class);
        } catch (Exception e) {
            log.warn("Can't parse webhook update: {}", e.getMessage());
            return ResponseEntity.ok().build(); // 200, иначе Telegram будет слать его снова
        }

        executor.submit(() -> {
            try {
                plannerBot.consume(update);
            } catch (Exception e) {
                log.error("Update {} failed", update.getUpdateId(), e);
            }
        });
        return ResponseEntity.ok().build();
    }

    private boolean secretMatches(String secret) {
        return secret != null && MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                props.webhookSecret().getBytes(StandardCharsets.UTF_8));
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}