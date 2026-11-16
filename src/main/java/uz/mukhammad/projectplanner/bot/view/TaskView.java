package uz.mukhammad.projectplanner.bot.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.task.Task;

@Component
@RequiredArgsConstructor
public class TaskView {

    private final ProjectView projectView;

    public String card(Task t) {
        StringBuilder sb = new StringBuilder();
        sb.append("📌 ").append(t.getKey()).append(" · ")
                .append(t.getPriority().getIcon()).append(" ").append(t.getPriority().getLabel()).append("\n");
        sb.append(t.getTitle()).append("\n\n");
        sb.append("Project: ").append(t.getProject().getCode()).append("\n");
        sb.append("Responsible: ").append(projectView.mention(t.getAssignee())).append("\n");
        sb.append("Deadline: ").append(t.getDeadline().format(DateParser.FORMAT)).append("\n");
        sb.append("Status: ").append(t.getStatus()).append("\n");
        sb.append("Progress: ").append(t.getProgress()).append("%\n");
        sb.append("Deliverable: ").append(t.getDeliverable() != null ? t.getDeliverable() : "⚠️ not set");
        return sb.toString();
    }
}
