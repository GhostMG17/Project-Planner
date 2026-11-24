package uz.mukhammad.projectplanner.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uz.mukhammad.projectplanner.IntegrationTest;
import uz.mukhammad.projectplanner.TestFixtures;
import uz.mukhammad.projectplanner.notification.repository.NotificationRepository;
import uz.mukhammad.projectplanner.notification.service.DeadlineReminderJob;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.service.TaskService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;

import uz.mukhammad.projectplanner.task.enums.TaskStatus;
import static org.assertj.core.api.Assertions.assertThat;

class DeadlineReminderIT extends IntegrationTest {

    @Autowired private TestFixtures f;
    @Autowired private ProjectService projectService;
    @Autowired private TaskService taskService;
    @Autowired private DeadlineReminderJob job;
    @Autowired private NotificationRepository notificationRepository;

    private User pm;
    private User member;
    private Project project;

    @BeforeEach
    void setUp() {
        User admin = f.user("admin", Role.ADMIN);
        pm = f.user("pm", Role.PROJECT_MANAGER);
        member = f.user("member", Role.TEAM_MEMBER);
        project = f.project("REM", pm, admin);
        projectService.addMember(pm, "REM", "@member");
    }

    @Test
    void dueTomorrowNotifiesAssigneeAndPmExactlyOnce() {
        f.task(project, member, pm, TestFixtures.today().plusDays(1));

        assertThat(job.runNow()).isEqualTo(2);   // ответственный + PM
        assertThat(job.runNow()).isZero();       // повторный запуск — дублей нет
        assertThat(notificationRepository.count()).isEqualTo(2);
    }

    @Test
    void completedTaskGetsNoReminder() {
        Task task = f.task(project, member, pm, TestFixtures.today().plusDays(1));
        taskService.changeStatus(member, task.getId(), TaskStatus.COMPLETED, null);

        assertThat(job.runNow()).isZero();
    }
}
