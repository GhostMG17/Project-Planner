package uz.mukhammad.projectplanner.unit;

import org.junit.jupiter.api.Test;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.common.DateParser;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateParserTest {

    @Test
    void parsesValidDate() {
        assertThat(DateParser.parse("15-11-2026", "deadline")).isEqualTo(LocalDate.of(2026, 11, 15));
    }

    @Test
    void rejectsNonExistentDate() {
        // без STRICT Java молча превратила бы это в 28 февраля
        assertThatThrownBy(() -> DateParser.parse("31-02-2026", "deadline"))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("Invalid deadline");
    }

    @Test
    void rejectsWrongFormat() {
        assertThatThrownBy(() -> DateParser.parse("2026-11-15", "deadline"))
                .isInstanceOf(BotException.class);
    }
}