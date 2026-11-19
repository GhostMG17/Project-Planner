package uz.mukhammad.projectplanner.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.mukhammad.projectplanner.bot.view.TaskView;
import uz.mukhammad.projectplanner.common.DateParser;
import uz.mukhammad.projectplanner.config.AppProperties;
import uz.mukhammad.projectplanner.config.NotificationProperties;
import uz.mukhammad.projectplanner.notification.enums.NotificationTrigger;
import uz.mukhammad.projectplanner.project.enums.ProjectStatus;
import uz.mukhammad.projectplanner.task.entity.Task;
import uz.mukhammad.projectplanner.task.enums.TaskStatus;
import uz.mukhammad.projectplanner.task.repository.TaskRepository;
import uz.mukhammad.projectplanner.user.User;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineReminderJob {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final ProjectRiskChecker projectRiskChecker;
    private final TaskView taskView;
    private final AppProperties appProperties;
    private final NotificationProperties notificationProperties;

    /** По расписанию — каждый час */
    @Scheduled(cron = "${app.notifications.cron}", zone = "${app.timezone}")
    public void scheduledRun() {
        safeRun(false);
    }

    /** Сразу после запуска — догоняем пропущенное, пока сервер был выключен */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        safeRun(false);
    }

    /** Ручной запуск (команда админа): игнорирует окно рабочих часов */
    public int runNow() {
        return run(true);
    }

    private void safeRun(boolean ignoreWindow) {
        try {
            run(ignoreWindow);
        } catch (Exception e) {
            log.error("Deadline reminder run failed", e);
        }
    }

    private synchronized int run(boolean ignoreWindow) {
        ZonedDateTime now = ZonedDateTime.now(appProperties.timezone());
        int hour = now.getHour();
        if (!ignoreWindow
                && (hour < notificationProperties.sendFromHour() || hour >= notificationProperties.sendToHour())) {
            log.debug("Outside send window ({}:00), reminders skipped", hour);
            return 0;
        }

        LocalDate today = now.toLocalDate();
        int sent = 0;
        sent += remindDue(today.plusDays(2), NotificationTrigger.DEADLINE_IN_2_DAYS);
        sent += remindDue(today.plusDays(1), NotificationTrigger.DEADLINE_TOMORROW);
        sent += alertOverdue(today);
        sent += projectRiskChecker.checkAll();

        log.info("Deadline reminders run finished: {} notification(s) sent", sent);
        return sent;
    }


    private int remindDue(LocalDate deadline, NotificationTrigger trigger) {
        int sent = 0;
        String when = trigger == NotificationTrigger.DEADLINE_TOMORROW ? "TOMORROW" : "in 2 days";

        for (Task t : openTasks(taskRepository.findByStatusNotAndDeadline(TaskStatus.COMPLETED, deadline))) {
            User assignee = t.getAssignee();
            String text = "⏰ " + t.getKey() + " is due " + when + " (" + t.getDeadline().format(DateParser.FORMAT) + ")\n"
                    + t.getTitle() + "\n\n" + details(t);

            if (notificationService.notifyUser(trigger, assignee, t,
                    key(trigger, t, "user", assignee.getId()), text, taskView.keyboard(t))) {
                sent++;
            }

            User pm = t.getProject().getProjectManager();
            if (trigger == NotificationTrigger.DEADLINE_TOMORROW && !pm.getId().equals(assignee.getId())) {
                String pmText = "⏰ " + t.getKey() + " (" + assignee.getShortName() + ") is due TOMORROW\n"
                        + t.getTitle() + "\n\n" + details(t);
                if (notificationService.notifyUser(trigger, pm, t,
                        key(trigger, t, "user", pm.getId()), pmText, taskView.keyboard(t))) {
                    sent++;
                }
            }
        }
        return sent;
    }


    private int alertOverdue(LocalDate today) {
        int sent = 0;
        NotificationTrigger trigger = NotificationTrigger.OVERDUE;

        for (Task t : openTasks(taskRepository.findByStatusNotAndDeadlineBefore(TaskStatus.COMPLETED, today))) {
            long days = ChronoUnit.DAYS.between(t.getDeadline(), today);
            User assignee = t.getAssignee();
            User pm = t.getProject().getProjectManager();
            String text = "🚨 " + t.getKey() + " is OVERDUE by " + days + (days == 1 ? " day" : " days")
                    + " (deadline " + t.getDeadline().format(DateParser.FORMAT) + ")\n"
                    + t.getTitle() + "\n\nResponsible: " + assignee.getShortName() + "\n" + details(t);

            if (notificationService.notifyUser(trigger, assignee, t,
                    key(trigger, t, "user", assignee.getId()), text, taskView.keyboard(t))) {
                sent++;
            }
            if (!pm.getId().equals(assignee.getId())
                    && notificationService.notifyUser(trigger, pm, t,
                    key(trigger, t, "user", pm.getId()), text, taskView.keyboard(t))) {
                sent++;
            }
            if (notificationService.notifyTopic(trigger, t.getProject(), t,
                    key(trigger, t, "topic", t.getProject().getId()), text)) {
                sent++;
            }
        }
        return sent;
    }


    /** Только задачи живых проектов */
    private List<Task> openTasks(List<Task> tasks) {
        return tasks.stream()
                .filter(t -> t.getProject().getStatus() == ProjectStatus.ACTIVE
                        || t.getProject().getStatus() == ProjectStatus.ON_HOLD)
                .toList();
    }

    private String details(Task t) {
        return "Project: " + t.getProject().getCode()
                + " · " + taskView.statusLabel(t.getStatus())
                + " · " + t.getProgress() + "%";
    }

    private String key(NotificationTrigger trigger, Task t, String target, Long targetId) {
        return trigger + ":" + t.getId() + ":" + t.getDeadline() + ":" + target + ":" + targetId;
    }
}
