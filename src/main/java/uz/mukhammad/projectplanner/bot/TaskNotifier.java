package uz.mukhammad.projectplanner.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.mukhammad.projectplanner.bot.view.TaskView;
import uz.mukhammad.projectplanner.project.Project;
import uz.mukhammad.projectplanner.task.*;
import uz.mukhammad.projectplanner.user.User;

@Component
@RequiredArgsConstructor
public class TaskNotifier {

    private final MessageSender messageSender;
    private final TaskView taskView;

    public void statusChanged(StatusChange change, User actor) {
        Task t = change.task();
        Project p = t.getProject();

        String text = switch (t.getStatus()) {
            case COMPLETED -> "✅ " + t.getKey() + " completed by " + actor.getDisplayName()
                    + "\n" + t.getTitle();
            case BLOCKED -> "⛔ " + t.getKey() + " is BLOCKED by " + actor.getDisplayName()
                    + "\n" + t.getTitle() + "\nReason: " + t.getBlockedReason();
            default -> "🔄 " + t.getKey() + ": " + taskView.statusLabel(change.oldStatus())
                    + " → " + taskView.statusLabel(t.getStatus())
                    + " (by " + actor.getDisplayName() + ")\n" + t.getTitle();
        };

        messageSender.send(p.getTelegramChatId(), p.getTelegramThreadId(), text);

        User pm = p.getProjectManager();
        boolean important = t.getStatus() == TaskStatus.BLOCKED || t.getStatus() == TaskStatus.COMPLETED;
        if (important && !pm.getId().equals(actor.getId())) {
            messageSender.send(pm.getTelegramId(), null, "📬 " + text, taskView.keyboard(t));
        }

        User assignee = t.getAssignee();
        if (!assignee.getId().equals(actor.getId()) && !assignee.getId().equals(pm.getId())) {
            messageSender.send(assignee.getTelegramId(), null, "📬 " + text, taskView.keyboard(t));
        }
    }

    public void fieldChanged(FieldChange change, String label, User actor) {
        Task t = change.task();
        Project p = t.getProject();
        String text = "✏️ " + t.getKey() + ": " + label + " " + change.oldValue() + " → " + change.newValue()
                + " (by " + actor.getShortName() + ")\n" + t.getTitle();

        messageSender.send(p.getTelegramChatId(), p.getTelegramThreadId(), text);

        User assignee = t.getAssignee();
        if (!assignee.getId().equals(actor.getId())) {
            messageSender.send(assignee.getTelegramId(), null, "📬 " + text, taskView.keyboard(t));
        }
    }

    public void reassigned(Reassignment r, User actor) {
        Task t = r.task();
        Project p = t.getProject();
        User oldA = r.oldAssignee();
        User newA = t.getAssignee();

        messageSender.send(p.getTelegramChatId(), p.getTelegramThreadId(),
                "👤 " + t.getKey() + " reassigned: " + oldA.getShortName() + " → " + newA.getShortName()
                        + " (by " + actor.getShortName() + ")\n" + t.getTitle());

        if (!newA.getId().equals(actor.getId())) {
            messageSender.send(newA.getTelegramId(), null,
                    "📬 Task assigned to you by " + actor.getDisplayName() + "\n\n" + taskView.card(t),
                    taskView.keyboard(t));
        }
        if (!oldA.getId().equals(actor.getId())) {
            messageSender.send(oldA.getTelegramId(), null,
                    "ℹ️ " + t.getKey() + " was reassigned to " + newA.getShortName() + ".\n" + t.getTitle());
        }
    }


}
