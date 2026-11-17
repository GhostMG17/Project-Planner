package uz.mukhammad.projectplanner.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uz.mukhammad.projectplanner.common.BotException;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum TaskField {

    TITLE("title", "Title"),
    DESCRIPTION("desc", "Description"),
    DELIVERABLE("deliv", "Deliverable"),
    START_DATE("start", "Start date"),
    DEADLINE("deadline", "Deadline");

    private final String key;
    private final String label;

    public static TaskField fromKey(String key) {
        return Arrays.stream(values())
                .filter(f -> f.key.equals(key))
                .findFirst()
                .orElseThrow(() -> new BotException("Unknown field."));
    }
}
