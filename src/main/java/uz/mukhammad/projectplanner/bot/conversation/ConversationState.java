package uz.mukhammad.projectplanner.bot.conversation;

import java.time.Instant;
import java.util.Map;

public record ConversationState(String handler, String step, Long entityId,
                                Map<String, String> data, Instant expiresAt) {

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
