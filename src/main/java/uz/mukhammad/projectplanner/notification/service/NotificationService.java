package uz.mukhammad.projectplanner.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.notification.entity.Notification;
import uz.mukhammad.projectplanner.notification.enums.NotificationChannel;
import uz.mukhammad.projectplanner.notification.enums.NotificationTrigger;
import uz.mukhammad.projectplanner.notification.repository.NotificationRepository;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.user.User;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final MessageSender messageSender;

    public boolean notifyUser(NotificationTrigger trigger, User user, Task task,
                              String dedupKey, String text, InlineKeyboardMarkup keyboard) {
        return deliver(dedupKey,
                () -> new Notification(trigger, NotificationChannel.DM, user, task, task.getProject(), dedupKey),
                () -> messageSender.send(user.getTelegramId(), null, text, keyboard));
    }

    public boolean notifyTopic(NotificationTrigger trigger, Project project, Task task,
                               String dedupKey, String text) {
        return deliver(dedupKey,
                () -> new Notification(trigger, NotificationChannel.TOPIC, null, task, project, dedupKey),
                () -> messageSender.send(project.getTelegramChatId(), project.getTelegramThreadId(), text));
    }

    public boolean notifyUserAboutProject(NotificationTrigger trigger, User user, Project project,
                                          String dedupKey, String text, InlineKeyboardMarkup keyboard) {
        return deliver(dedupKey,
                () -> new Notification(trigger, NotificationChannel.DM, user, null, project, dedupKey),
                () -> messageSender.send(user.getTelegramId(), null, text, keyboard));
    }

    private boolean deliver(String dedupKey, Supplier<Notification> factory, BooleanSupplier sender) {
        Optional<Notification> existing = repository.findByDedupKey(dedupKey);
        if (existing.isPresent() && !existing.get().shouldSend()) {
            return false;
        }

        Notification notification = existing.orElseGet(factory);
        boolean success = sender.getAsBoolean();
        notification.markResult(success);
        repository.save(notification);

        if (success) {
            log.info("Notification sent: {}", dedupKey);
        } else {
            log.warn("Notification failed (attempt {}): {}", notification.getAttempts(), dedupKey);
        }
        return success;
    }
}
