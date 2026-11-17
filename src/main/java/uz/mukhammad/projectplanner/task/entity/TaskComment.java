package uz.mukhammad.projectplanner.task.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uz.mukhammad.projectplanner.task.enums.FileType;
import uz.mukhammad.projectplanner.user.User;

import java.time.Instant;

@Entity
@Table(name = "task_comments")
@Getter
@NoArgsConstructor
public class TaskComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    private String body;

    private String fileId;

    @Enumerated(EnumType.STRING)
    private FileType fileType;

    private String fileName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public TaskComment(Task task, User author, String body,
                       String fileId, FileType fileType, String fileName) {
        this.task = task;
        this.author = author;
        this.body = body;
        this.fileId = fileId;
        this.fileType = fileType;
        this.fileName = fileName;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public boolean hasFile() {
        return fileId != null;
    }
}
