package uz.mukhammad.projectplanner.bot.conversation;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConversationStore {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final Map<ConversationKey, ConversationState> states = new ConcurrentHashMap<>();

    public void start(ConversationKey key, String handler, String step, Long entityId) {
        states.values().removeIf(ConversationState::isExpired);
        states.put(key, new ConversationState(handler, step, entityId, Instant.now().plus(TTL)));
    }

    public Optional<ConversationState> get(ConversationKey key) {
        ConversationState state = states.get(key);
        if (state == null) {
            return Optional.empty();
        }
        if (state.isExpired()) {
            states.remove(key);
            return Optional.empty();
        }
        return Optional.of(state);
    }

    public void clear(ConversationKey key) {
        states.remove(key);
    }
}
