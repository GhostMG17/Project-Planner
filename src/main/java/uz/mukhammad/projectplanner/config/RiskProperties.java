package uz.mukhammad.projectplanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.risk")
public record RiskProperties(int threshold) {}
