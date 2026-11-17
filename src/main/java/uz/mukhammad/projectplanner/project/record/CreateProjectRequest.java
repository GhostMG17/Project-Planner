package uz.mukhammad.projectplanner.project.record;

import java.time.LocalDate;

public record CreateProjectRequest(
        String code,
        String name,
        String client,
        String pmUsername,
        LocalDate startDate,
        LocalDate plannedEndDate
) {}
