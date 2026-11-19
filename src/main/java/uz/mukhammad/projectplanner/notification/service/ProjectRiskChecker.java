package uz.mukhammad.projectplanner.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uz.mukhammad.projectplanner.bot.view.ProjectView;
import uz.mukhammad.projectplanner.notification.enums.NotificationTrigger;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;
import uz.mukhammad.projectplanner.project.repository.ProjectRepository;
import uz.mukhammad.projectplanner.risk.records.ProjectHealth;
import uz.mukhammad.projectplanner.risk.service.ProjectHealthService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;
import uz.mukhammad.projectplanner.user.UserRepository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectRiskChecker {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectHealthService healthService;
    private final NotificationService notificationService;
    private final ProjectView projectView;

    public int checkAll() {
        int sent = 0;
        for (Project p : projectRepository.findByStatusIn(List.of(ProjectStatus.ACTIVE, ProjectStatus.ON_HOLD))) {
            sent += check(p);
        }
        return sent;
    }

    public int check(Project p) {
        ProjectHealth health = healthService.assess(p);
        int sent = 0;

        if (health.atRisk()) {
            Instant since = p.getAtRiskSince();
            if (since == null) {
                since = Instant.now();
                projectRepository.updateAtRiskSince(p.getId(), since);
                log.info("Project {} became AT RISK: {}", p.getCode(), health.reason());
            }

            String text = "🔴 Project " + p.getCode() + " · " + p.getName() + " is AT RISK\n\n"
                    + "Reason: " + health.reason() + "\n"
                    + "PM: " + p.getProjectManager().getShortName() + "\n\n"
                    + "Details: /overdue or /project " + p.getCode();

            for (User lead : leads()) {
                String key = "PROJECT_AT_RISK:" + p.getId() + ":" + since.toEpochMilli() + ":user:" + lead.getId();
                if (notificationService.notifyUserAboutProject(NotificationTrigger.PROJECT_AT_RISK,
                        lead, p, key, text, projectView.cardKeyboard(p, true))) {
                    sent++;
                }
            }
        } else if (p.getAtRiskSince() != null) {
            Instant since = p.getAtRiskSince();
            projectRepository.updateAtRiskSince(p.getId(), null);
            log.info("Project {} is back on track", p.getCode());

            String text = "✅ Project " + p.getCode() + " · " + p.getName() + " is back on track.";
            for (User lead : leads()) {
                String key = "PROJECT_BACK_ON_TRACK:" + p.getId() + ":" + since.toEpochMilli() + ":user:" + lead.getId();
                if (notificationService.notifyUserAboutProject(NotificationTrigger.PROJECT_BACK_ON_TRACK,
                        lead, p, key, text, null)) {
                    sent++;
                }
            }
        }
        return sent;
    }

    private List<User> leads() {
        return userRepository.findByRoleInAndActiveTrue(List.of(Role.PORTFOLIO_LEAD, Role.ADMIN));
    }
}