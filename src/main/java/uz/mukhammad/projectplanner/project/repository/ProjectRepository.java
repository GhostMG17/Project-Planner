package uz.mukhammad.projectplanner.project.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.user.User;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "projectManager")
    List<Project> findAllByOrderByCodeAsc();

    @EntityGraph(attributePaths = "projectManager")
    Optional<Project> findByCode(String code);

    @EntityGraph(attributePaths = "projectManager")
    @Query("""
        select p from Project p
        where p.projectManager = :user
           or exists (select m.id from ProjectMember m where m.project = p and m.user = :user)
        order by p.code
        """)
    List<Project> findVisibleFor(@Param("user") User user);


    @EntityGraph(attributePaths = "projectManager")
    Optional<Project> findWithPmById(Long id);


}
