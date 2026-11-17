package uz.mukhammad.projectplanner.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uz.mukhammad.projectplanner.notification.enums.NotificationChannel;
import uz.mukhammad.projectplanner.notification.enums.NotificationStatus;
import uz.mukhammad.projectplanner.notification.enums.NotificationTrigger;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.user.User;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor
public class Notification {

    public static final int MAX_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private NotificationTrigger trigger;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false, unique = true)
    private String dedupKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant sentAt;

    public Notification(NotificationTrigger trigger, NotificationChannel channel,
                        User user, Task task, Project project, String dedupKey) {
        this.trigger = trigger;
        this.channel = channel;
        this.user = user;
        this.task = task;
        this.project = project;
        this.dedupKey = dedupKey;
        this.status = NotificationStatus.FAILED; // пока не отправлено
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void markResult(boolean success) {
        attempts++;
        status = success ? NotificationStatus.SENT : NotificationStatus.FAILED;
        sentAt = success ? Instant.now() : null;
    }

    public boolean shouldSend() {
        return status != NotificationStatus.SENT && attempts < MAX_ATTEMPTS;
    }
}
