package uz.mukhammad.projectplanner.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.mukhammad.projectplanner.config.AppProperties;

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
}
