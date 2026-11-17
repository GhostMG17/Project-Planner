package uz.mukhammad.projectplanner.project.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uz.mukhammad.projectplanner.common.BotException;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ProjectField {

    NAME("name", "Name"),
    CLIENT("client", "Client"),
    DESCRIPTION("desc", "Description"),
    DELIVERABLES("deliv", "Key deliverables"),
    START_DATE("start", "Start date"),
    END_DATE("end", "End date");

    private final String key;
    private final String label;

    public static ProjectField fromKey(String key) {
        return Arrays.stream(values())
                .filter(f -> f.key.equals(key))
                .findFirst()
                .orElseThrow(() -> new BotException("Unknown field."));
    }
}
