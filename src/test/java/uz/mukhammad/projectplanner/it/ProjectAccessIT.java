package uz.mukhammad.projectplanner.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uz.mukhammad.projectplanner.IntegrationTest;
import uz.mukhammad.projectplanner.TestFixtures;
import uz.mukhammad.projectplanner.common.BotException;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.project.service.ProjectService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;
import uz.mukhammad.projectplanner.user.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectAccessIT extends IntegrationTest {

    @Autowired private TestFixtures f;
    @Autowired private ProjectService projectService;
    @Autowired private UserService userService;

    private User admin;
    private User pm;
    private User member;
    private User outsider;
    private Project project;

    @BeforeEach
    void setUp() {
        admin = f.user("admin", Role.ADMIN);
        pm = f.user("pm", Role.PROJECT_MANAGER);
        member = f.user("member", Role.TEAM_MEMBER);
        outsider = f.user("outsider", Role.TEAM_MEMBER);
        project = f.project("ACC", pm, admin);
        projectService.addMember(pm, "ACC", "@member");
    }

    @Test
    void memberSeesOwnProjectOutsiderDoesNot() {
        assertThat(projectService.findVisibleProjects(member)).extracting(Project::getCode).containsExactly("ACC");
        assertThat(projectService.findVisibleProjects(outsider)).isEmpty();
    }

    @Test
    void adminSeesAllProjects() {
        f.project("OTH", pm, admin);
        assertThat(projectService.findVisibleProjects(admin)).hasSize(2);
    }

    @Test
    void outsiderCannotViewProject() {
        assertThatThrownBy(() -> projectService.getTeam(outsider, "ACC"))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void memberCannotManageTeam() {
        assertThatThrownBy(() -> projectService.addMember(member, "ACC", "@outsider"))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("Only the PM");
    }

    @Test
    void duplicateProjectCodeIsRejected() {
        assertThatThrownBy(() -> f.project("ACC", pm, admin))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void teamMemberCannotBecomeProjectManager() {
        assertThatThrownBy(() -> projectService.changeProjectManager(admin, "ACC", "@member"))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("can't be a Project Manager");
    }

    @Test
    void cannotDemotePmOfActiveProject() {
        assertThatThrownBy(() -> userService.changeRole(admin, "@pm", Role.TEAM_MEMBER))
                .isInstanceOf(BotException.class)
                .hasMessageContaining("ACC");
    }
}