package uz.mukhammad.projectplanner.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class DateParser {

    public static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);

    private DateParser() {}

    public static LocalDate parse(String value, String fieldName) {
        try {
            return LocalDate.parse(value.trim(), FORMAT);
        } catch (DateTimeParseException e) {
            throw new BotException("Invalid " + fieldName + ": '" + value.trim() + "'. Use format dd-MM-yyyy, e.g. 15-11-2026.");
        }
    }
}
