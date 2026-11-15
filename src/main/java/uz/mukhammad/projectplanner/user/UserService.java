package uz.mukhammad.projectplanner.user;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.config.AppProperties;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AppProperties appProperties;

    @Transactional
    public RegistrationResult registerOrUpdate(Long telegramId, String username,
                                               String firstName, String lastName){
        Optional<User> existing = userRepository.findByTelegramId(telegramId);
        boolean created = existing.isEmpty();

        User user = existing.orElseGet(()->{
            User u = new User();
            u.setTelegramId(telegramId);
            return u;
        });

        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        if (telegramId.equals(appProperties.adminTelegramId()) && user.getRole() != Role.ADMIN){
            user.setRole(Role.ADMIN);
            log.info("User {} granted ADMIN role from configuration", telegramId);
        }

        User saved = userRepository.save(user);
        if (created){
            log.info("New user registered: telegramId={}, username={}", telegramId, username);
        }
        return new RegistrationResult(saved, created);
    }


    @Transactional(readOnly = true)
    public List<User> findAllActive() {
        return userRepository.findAll().stream()
                .filter(User::isActive)
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    @Transactional
    public User changeRole(User actor, String targetUsername, Role newRole) {
        String username = targetUsername.startsWith("@") ? targetUsername.substring(1) : targetUsername;

        User target = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BotException(
                        "User @" + username + " not found. They must press /start in private chat first."));

        if (target.getId().equals(actor.getId())) {
            throw new BotException("You can't change your own role.");
        }

        Role oldRole = target.getRole();
        target.setRole(newRole);
        log.info("Role changed by {}: user {} {} -> {}",
                actor.getTelegramId(), target.getTelegramId(), oldRole, newRole);
        return target;
    }


}
