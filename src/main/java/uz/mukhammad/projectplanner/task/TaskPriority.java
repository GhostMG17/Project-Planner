package uz.mukhammad.projectplanner.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uz.mukhammad.projectplanner.common.BotException;

@Getter
@RequiredArgsConstructor
public enum TaskPriority {

    LOW("⚪", "Low"),
    MEDIUM("🟡", "Medium"),
    HIGH("🟠", "High"),
    CRITICAL("🔴", "Critical");

    private final String icon;
    private final String label;

    public static TaskPriority parse(String raw) {
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BotException("Unknown priority '" + raw + "'. Use: Low, Medium, High, Critical.");
        }
    }
}
