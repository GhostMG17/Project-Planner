package uz.mukhammad.projectplanner.task.enums;
import java.util.EnumSet;
import java.util.Set;

public enum TaskStatus {
    NEW, IN_PROGRESS, ON_HOLD, BLOCKED, COMPLETED;

    public Set<TaskStatus> next() {
        return switch (this) {
            case NEW -> EnumSet.of(IN_PROGRESS, ON_HOLD, BLOCKED, COMPLETED);
            case IN_PROGRESS -> EnumSet.of(ON_HOLD, BLOCKED, COMPLETED);
            case ON_HOLD -> EnumSet.of(IN_PROGRESS, BLOCKED, COMPLETED);
            case BLOCKED -> EnumSet.of(IN_PROGRESS, ON_HOLD);
            case COMPLETED -> EnumSet.of(IN_PROGRESS);
        };
    }
}
