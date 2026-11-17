package uz.mukhammad.projectplanner.project_member;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.user.User;

import java.time.Instant;

@Entity
@Table(name = "project_members")
@Getter
@NoArgsConstructor
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "added_by_id")
    private User addedBy;

    @Column(nullable = false, updatable = false)
    private Instant addedAt;

    public ProjectMember(Project project, User user, User addedBy) {
        this.project = project;
        this.user = user;
        this.addedBy = addedBy;
    }

    @PrePersist
    void onCreate() {
        addedAt = Instant.now();
    }
}
