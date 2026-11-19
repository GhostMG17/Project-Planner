package uz.mukhammad.projectplanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(
        String cron,
        int sendFromHour,
        int sendToHour,
        List<Integer> remindDaysBefore,
        int pmDaysBefore
) {
    public NotificationProperties {
        if (sendFromHour < 0 || sendToHour > 24 || sendFromHour >= sendToHour) {
            throw new IllegalArgumentException(
                    "app.notifications: send window must satisfy 0 <= from < to <= 24, got "
                            + sendFromHour + "–" + sendToHour);
        }
        if (remindDaysBefore == null || remindDaysBefore.isEmpty()
                || remindDaysBefore.stream().anyMatch(d -> d == null || d < 1)) {
            throw new IllegalArgumentException(
                    "app.notifications.remind-days-before must be a list of positive numbers, e.g. 2,1");
        }
        if (pmDaysBefore < 0) {
            throw new IllegalArgumentException("app.notifications.pm-days-before must be >= 0");
        }
        remindDaysBefore = List.copyOf(remindDaysBefore);
    }
}