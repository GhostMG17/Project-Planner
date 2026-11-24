package uz.mukhammad.projectplanner;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import uz.mukhammad.projectplanner.bot.TopicService;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(locations = "classpath:test.properties")
public abstract class IntegrationTest {

    // Один контейнер на все тестовые классы — поднимается один раз
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    static {
        POSTGRES.start();
    }

    private static final AtomicInteger TOPIC_IDS = new AtomicInteger(1000);

    @MockitoBean
    protected TelegramClient telegramClient;

    @MockitoBean
    protected TopicService topicService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void resetState() throws Exception {
        jdbc.execute("""
                TRUNCATE report_log, notifications, task_comments, task_history, tasks,
                         risks, milestones, project_members, projects, users
                RESTART IDENTITY CASCADE
                """);
        when(topicService.createTopic(any(), any())).thenAnswer(inv -> TOPIC_IDS.incrementAndGet());
    }
}