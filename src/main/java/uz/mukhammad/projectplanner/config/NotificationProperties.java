package uz.mukhammad.projectplanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(String cron, int sendFromHour, int sendToHour) {}