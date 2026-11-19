package uz.mukhammad.projectplanner.risk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.risk.enums.RiskSeverity;
import uz.mukhammad.projectplanner.risk.enums.RiskStatus;
import uz.mukhammad.projectplanner.user.User;

import java.time.Instant;

@Entity
@Table(name = "risks")
@Getter
@NoArgsConstructor
public class Risk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskSeverity severity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskStatus status = RiskStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant closedAt;

    public Risk(Project project, String description, RiskSeverity severity, User owner, User createdBy) {
        this.project = project;
        this.description = description;
        this.severity = severity;
        this.owner = owner;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void close() {
        status = RiskStatus.CLOSED;
        closedAt = Instant.now();
    }
}
