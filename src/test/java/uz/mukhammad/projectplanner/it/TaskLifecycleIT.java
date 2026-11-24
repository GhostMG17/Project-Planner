package uz.mukhammad.projectplanner.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uz.mukhammad.projectplanner.IntegrationTest;
import uz.mukhammad.projectplanner.TestFixtures;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.record.StatusChange;
import uz.mukhammad.projectplanner.task.repository.TaskHistoryRepository;
import uz.mukhammad.projectplanner.task.service.TaskService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;

import uz.mukhammad.projectplanner.task.enums.TaskStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;


class TaskLifecycleIT extends IntegrationTest {

    @Autowired private TestFixtures f;
    @Autowired private TaskService taskService;
    @Autowired private ProjectService projectService;
    @Autowired private TaskHistoryRepository historyRepository;

    private User pm;
    private User member;
    private User outsider;
    private Project project;

    @BeforeEach
    void setUp() {
        User admin = f.user("admin", Role.ADMIN);
        pm = f.user("pm", Role.PROJECT_MANAGER);
        member = f.user("member", Role.TEAM_MEMBER);
        outsider = f.user("outsider", Role.TEAM_MEMBER);
        project = f.project("TST", pm, admin);
        projectService.addMember(pm, "TST", "@member");
    }

    @Test
    void completingTaskSetsProgressTo100AndCompletedAt() {
        Task task = f.task(project, member, pm, TestFixtures.today().plusDays(5));

        StatusChange change = taskService.changeStatus(member, task.getId(), TaskStatus.COMPLETED, null);

        assertThat(change.task().getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(change.task().getProgress()).isEqualTo(100);
        assertThat(change.task().getCompletedAt()).isNotNull();
    }

    @Test
    void blockingRequiresReason() {
        Task task = f.task(project, member, pm, TestFixtures.today().plusDays(5));

        assertThatThrownBy(() -> taskService.changeStatus(member, task.getId(), TaskStatus.BLOCKED, "   "))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("why");
    }

    @Test
    void blockedTaskCannotBeCompletedDirectly() {
        Task task = f.task(project, member, pm, TestFixtures.today().plusDays(5));
        taskService.changeStatus(member, task.getId(), TaskStatus.BLOCKED, "Waiting for data");

        assertThatThrownBy(() -> taskService.changeStatus(member, task.getId(), TaskStatus.COMPLETED, null))
                .isInstanceOf(BotException.class);
    }

    @Test
    void memberCannotReopenButPmCanAndProgressDropsTo90() {
        Task task = f.task(project, member, pm, TestFixtures.today().plusDays(5));
        taskService.changeStatus(member, task.getId(), TaskStatus.COMPLETED, null);

        assertThatThrownBy(() -> taskService.changeStatus(member, task.getId(), TaskStatus.IN_PROGRESS, null))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("Only the PM");

        StatusChange reopened = taskService.changeStatus(pm, task.getId(), TaskStatus.IN_PROGRESS, null);
        assertThat(reopened.task().getProgress()).isEqualTo(90);
        assertThat(reopened.task().getCompletedAt()).isNull();
    }

    @Test
    void outsiderCannotChangeStatus() {
        Task task = f.task(project, member, pm, TestFixtures.today().plusDays(5));

        assertThatThrownBy(() -> taskService.changeStatus(outsider, task.getId(), TaskStatus.IN_PROGRESS, null))
                .isInstanceOf(BotException.class);
    }

    @Test
    void everyChangeIsRecordedInHistory() {
        Task task = f.task(project, member, pm, TestFixtures.today().plusDays(5));
        taskService.changeStatus(member, task.getId(), TaskStatus.IN_PROGRESS, null);
        taskService.changeStatus(member, task.getId(), TaskStatus.COMPLETED, null);

        // CREATED + STATUS(start) + STATUS(complete) + PROGRESS(0→100)
        assertThat(historyRepository.findByTask_ProjectOrderByChangedAtAscIdAsc(project)).hasSize(4);
    }

    @Test
    void cannotAssignTaskToSomeoneOutsideTheTeam() {
        assertThatThrownBy(() -> f.task(project, outsider, pm, TestFixtures.today().plusDays(5)))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("not in the TST team");
    }

    @Test
    void deadlineCannotBeInThePast() {
        assertThatThrownBy(() -> f.task(project, member, pm, TestFixtures.today().minusDays(1)))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("past");
    }
}