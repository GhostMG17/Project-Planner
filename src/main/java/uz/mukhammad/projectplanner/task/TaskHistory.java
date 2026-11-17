package uz.mukhammad.projectplanner.task;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uz.mukhammad.projectplanner.user.User;

import java.time.Instant;

@Entity
@Table(name = "task_history")
@Getter
@NoArgsConstructor
public class TaskHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_id")
    private User changedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeType changeType;

    private String oldValue;

    private String newValue;

    private String comment;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    public TaskHistory(Task task, User changedBy, ChangeType changeType,
                       String oldValue, String newValue, String comment) {
        this.task = task;
        this.changedBy = changedBy;
        this.changeType = changeType;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.comment = comment;
    }

    @PrePersist
    void onCreate() {
        changedAt = Instant.now();
    }
}
