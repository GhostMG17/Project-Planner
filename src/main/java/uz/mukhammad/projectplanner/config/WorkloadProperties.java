package uz.mukhammad.projectplanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.workload")
public record WorkloadProperties(int overloadThreshold) {

    public WorkloadProperties {
        if (overloadThreshold < 1) {
            throw new IllegalArgumentException("app.workload.overload-threshold must be >= 1");
        }
    }
}
