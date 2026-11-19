package uz.mukhammad.projectplanner.notification.enums;

public enum NotificationTrigger {
    @Deprecated DEADLINE_IN_2_DAYS,
    @Deprecated DEADLINE_TOMORROW,
    DEADLINE_REMINDER,
    OVERDUE,
    PROJECT_AT_RISK,
    PROJECT_BACK_ON_TRACK
}