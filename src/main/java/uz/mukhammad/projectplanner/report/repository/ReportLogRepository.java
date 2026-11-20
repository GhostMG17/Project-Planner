package uz.mukhammad.projectplanner.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.mukhammad.projectplanner.project.entity.Project;
import uz.mukhammad.projectplanner.report.entity.ReportLog;
import uz.mukhammad.projectplanner.user.User;

import java.util.Optional;

public interface ReportLogRepository extends JpaRepository<ReportLog, Long> {

    Optional<ReportLog> findTopByProjectAndUserOrderByCreatedAtDesc(Project project, User user);
}
