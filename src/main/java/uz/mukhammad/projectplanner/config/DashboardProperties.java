package uz.mukhammad.projectplanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.support.CronExpression;

@ConfigurationProperties(prefix = "app.dashboard")
public record DashboardProperties(Integer threadId, String dailyCron, String weeklyCron) {

    public DashboardProperties {
        if (threadId == null) {
            throw new IllegalArgumentException("app.dashboard.thread-id (DASHBOARD_THREAD_ID) is required");
        }
        CronExpression.parse(dailyCron);
        CronExpression.parse(weeklyCron);
    }
}
