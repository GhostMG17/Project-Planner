package uz.mukhammad.projectplanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.record.CreateProjectRequest;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.enums.TaskPriority;
import uz.mukhammad.projectplanner.task.record.CreateTaskRequest;
import uz.mukhammad.projectplanner.task.service.TaskService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;
import uz.mukhammad.projectplanner.user.UserRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TestFixtures {

    public static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");
    private static final AtomicLong TELEGRAM_IDS = new AtomicLong(100);

    @Autowired private UserRepository userRepository;
    @Autowired private ProjectService projectService;
    @Autowired private TaskService taskService;

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public User user(String username, Role role) {
        User u = new User();
        u.setTelegramId(TELEGRAM_IDS.incrementAndGet());
        u.setUsername(username);
        u.setFirstName(username);
        u.setRole(role);
        return userRepository.save(u);
    }

    public Project project(String code, User pm, User creator) {
        return projectService.createProject(creator, new CreateProjectRequest(
                code, "Project " + code, "Client", "@" + pm.getUsername(),
                today().minusDays(10), today().plusMonths(3)));
    }

    public Task task(Project project, User assignee, User creator, LocalDate deadline) {
        return taskService.createTask(creator, new CreateTaskRequest(
                project.getCode(), "Test task", assignee.getId(), deadline, TaskPriority.MEDIUM, "Output"));
    }
}