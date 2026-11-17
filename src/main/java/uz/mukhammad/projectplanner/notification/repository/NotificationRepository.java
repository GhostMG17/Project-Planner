package uz.mukhammad.projectplanner.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.mukhammad.projectplanner.notification.entity.Notification;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByDedupKey(String dedupKey);
}
