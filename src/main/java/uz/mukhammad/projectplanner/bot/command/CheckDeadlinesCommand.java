package uz.mukhammad.projectplanner.bot.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.notification.service.DeadlineReminderJob;
import uz.mukhammad.projectplanner.user.AccessService;
import uz.mukhammad.projectplanner.user.Role;
import uz.mukhammad.projectplanner.user.User;

@Component
@RequiredArgsConstructor
public class CheckDeadlinesCommand implements CommandHandler {

    private final AccessService accessService;
    private final DeadlineReminderJob deadlineReminderJob;
    private final MessageSender messageSender;

    @Override public String command() { return "/checkdeadlines"; }
    @Override public String description() { return "Run deadline reminders now (admin)"; }

    @Override
    public void handle(Message message) {
        User actor = accessService.requireUser(message.getFrom().getId());
        accessService.requireRole(actor, Role.ADMIN);

        int sent = deadlineReminderJob.runNow();
        messageSender.reply(message, "⏰ Deadline check finished: " + sent + " notification(s) sent.");
    }
}
