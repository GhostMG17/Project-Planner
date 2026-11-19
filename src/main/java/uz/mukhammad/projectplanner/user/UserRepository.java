package uz.mukhammad.projectplanner.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByTelegramId(Long telegramId);

    Optional<User> findByUsernameIgnoreCase(String username);

    List<User> findByRoleInAndActiveTrue(Collection<Role> roles);
}
