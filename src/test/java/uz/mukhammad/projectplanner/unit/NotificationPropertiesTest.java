package uz.mukhammad.projectplanner.unit;
import org.junit.jupiter.api.Test;
import uz.mukhammad.projectplanner.config.NotificationProperties;


import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

class NotificationPropertiesTest {

    @Test
    void validSettingsAreAccepted() {
        assertThatCode(() -> new NotificationProperties("0 0 * * * *", 9, 20, List.of(2, 1), 1))
                .doesNotThrowAnyException();
    }

    @Test
    void zeroDaysBeforeIsRejected() {
        assertThatThrownBy(() -> new NotificationProperties("0 0 * * * *", 9, 20, List.of(0, 1), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remind-days-before");
    }

    @Test
    void invertedSendWindowIsRejected() {
        assertThatThrownBy(() -> new NotificationProperties("0 0 * * * *", 20, 9, List.of(1), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}