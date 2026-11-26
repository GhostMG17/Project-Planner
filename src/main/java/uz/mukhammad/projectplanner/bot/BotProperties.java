package uz.mukhammad.projectplanner.bot;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegram.bot")
public record BotProperties(String token,
                            String username,
                            Long groupChatId,
                            String mode,
                            String webhookUrl,
                            String webhookSecret) {

    public BotProperties {
        if (mode == null || mode.isBlank()) {
            mode = "polling";
        }
        if (!mode.equals("polling") && !mode.equals("webhook") && !mode.equals("none")) {
            throw new IllegalArgumentException("telegram.bot.mode must be polling, webhook or none");
        }
        if (mode.equals("webhook")) {
            if (webhookUrl == null || !webhookUrl.startsWith("https://")) {
                throw new IllegalArgumentException("TELEGRAM_WEBHOOK_URL must start with https:// in webhook mode");
            }
            if (webhookSecret == null || !webhookSecret.matches("[A-Za-z0-9_-]{16,256}")) {
                throw new IllegalArgumentException("TELEGRAM_WEBHOOK_SECRET: 16–256 chars, only A-Z a-z 0-9 _ -");
            }
        }
    }
}