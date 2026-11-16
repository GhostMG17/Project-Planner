package uz.mukhammad.projectplanner.bot.conversation;

import java.time.Instant;

public record ConversationState(String handler, String step, Long entityId, Instant expiresAt) {

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
