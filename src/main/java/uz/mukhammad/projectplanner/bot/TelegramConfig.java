package uz.mukhammad.projectplanner.bot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class TelegramConfig {

    @Bean
    public TelegramClient telegramClient(BotProperties props){
        return new OkHttpTelegramClient(props.token());
    }

    @Bean(destroyMethod = "close")
    public TelegramBotsLongPollingApplication botsApplication() {
        return new TelegramBotsLongPollingApplication();
    }

}
