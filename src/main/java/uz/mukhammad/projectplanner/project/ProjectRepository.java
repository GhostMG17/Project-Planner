package uz.mukhammad.projectplanner.project;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "projectManager")
    List<Project> findAllByOrderByCodeAsc();

    @EntityGraph(attributePaths = "projectManager")
    List<Project> findByProjectManagerOrderByCodeAsc(User projectManager);

}
