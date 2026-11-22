package uz.mukhammad.projectplanner.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import uz.mukhammad.projectplanner.bot.BotProperties;
import uz.mukhammad.projectplanner.bot.MessageSender;
import uz.mukhammad.projectplanner.bot.view.DashboardView;
import uz.mukhammad.projectplanner.config.AppProperties;
import uz.mukhammad.projectplanner.config.DashboardProperties;
import uz.mukhammad.projectplanner.dashboard.records.DailyOverview;
import uz.mukhammad.projectplanner.dashboard.records.WeeklyReport;
import uz.mukhammad.projectplanner.dashboard.service.DashboardService;
import uz.mukhammad.projectplanner.notification.enums.NotificationTrigger;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardPublisher {

    private final DashboardService dashboardService;
    private final DashboardView dashboardView;
    private final NotificationService notificationService;
    private final MessageSender messageSender;
    private final BotProperties botProperties;
    private final DashboardProperties dashboardProperties;
    private final AppProperties appProperties;


    @Scheduled(cron = "${app.dashboard.daily-cron}", zone = "${app.timezone}")
    public void scheduledDaily() {
        safely(() -> publishDaily(false), "daily overview");
    }

    @Scheduled(cron = "${app.dashboard.weekly-cron}", zone = "${app.timezone}")
    public void scheduledWeekly() {
        safely(() -> publishWeekly(false), "weekly report");
    }


    @EventListener(ApplicationReadyEvent.class)
    public void catchUp() {
        ZonedDateTime now = ZonedDateTime.now(appProperties.timezone());
        LocalDate today = now.toLocalDate();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        if (missed(dashboardProperties.dailyCron(), today, now)) {
            safely(() -> publishDaily(false), "daily overview (catch-up)");
        }
        if (missed(dashboardProperties.weeklyCron(), weekStart, now)) {
            safely(() -> publishWeekly(false), "weekly report (catch-up)");
        }
    }


    public void publishDaily(boolean force) {
        DailyOverview d = dashboardService.daily();
        String text = dashboardView.daily(d);
        String key = "DAILY_OVERVIEW:" + d.today();
        publish(NotificationTrigger.DAILY_OVERVIEW, key, text, force);
    }

    public void publishWeekly(boolean force) {
        WeeklyReport w = dashboardService.weekly();
        String text = dashboardView.weekly(w);
        String key = "WEEKLY_REPORT:" + w.weekStart();
        publish(NotificationTrigger.WEEKLY_REPORT, key, text, force);
    }

    private void publish(NotificationTrigger trigger, String key, String text, boolean force) {
        Long chatId = botProperties.groupChatId();
        Integer threadId = dashboardProperties.threadId();
        if (force) {
            messageSender.sendLong(chatId, threadId, text, null);
            log.info("{} published manually", trigger);
        } else if (notificationService.notifyDashboard(trigger, key, chatId, threadId, text)) {
            log.info("{} published: {}", trigger, key);
        }
    }

    private boolean missed(String cron, LocalDate periodStart, ZonedDateTime now) {
        ZonedDateTime from = periodStart.atStartOfDay(now.getZone()).minusSeconds(1);
        ZonedDateTime firstFire = CronExpression.parse(cron).next(from);
        return firstFire != null && firstFire.isBefore(now);
    }

    private void safely(Runnable action, String what) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("Failed to publish {}", what, e);
        }
    }
}
