package uz.mukhammad.projectplanner.milestone.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uz.mukhammad.projectplanner.milestone.enums.MilestoneStatus;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.user.User;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "milestones")
@Getter
@NoArgsConstructor
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MilestoneStatus status = MilestoneStatus.PLANNED;

    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Milestone(Project project, String name, LocalDate dueDate, User createdBy) {
        this.project = project;
        this.name = name;
        this.dueDate = dueDate;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void toggle() {
        if (status == MilestoneStatus.PLANNED) {
            status = MilestoneStatus.COMPLETED;
            completedAt = Instant.now();
        } else {
            status = MilestoneStatus.PLANNED;
            completedAt = null;
        }
    }

    /** Запланирована, а дата уже прошла */
    public boolean isMissed(LocalDate today) {
        return status == MilestoneStatus.PLANNED && dueDate.isBefore(today);
    }
}