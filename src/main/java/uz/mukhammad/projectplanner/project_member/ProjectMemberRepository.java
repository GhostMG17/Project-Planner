package uz.mukhammad.projectplanner.project_member;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectAndUser(Project project, User user);

    Optional<ProjectMember> findByProjectAndUser(Project project, User user);

    @EntityGraph(attributePaths = "user")
    List<ProjectMember> findByProjectOrderByAddedAtAsc(Project project);
}
