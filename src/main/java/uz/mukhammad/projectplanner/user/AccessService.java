package uz.mukhammad.projectplanner.user;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.mukhammad.projectplanner.common.BotException;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccessService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User requireUser(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new BotException(
                        "You're not registered. Open a private chat with me and press /start."));
        if (!user.isActive()) {
            throw new BotException("Your account is deactivated. Contact the administrator.");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public Optional<User> findUser(Long telegramId) {
        return userRepository.findByTelegramId(telegramId).filter(User::isActive);
    }

    public void requireRole(User user, Role... allowed) {
        if (!Arrays.asList(allowed).contains(user.getRole())) {
            throw new BotException("You don't have permission for this action.");
        }
    }
}
