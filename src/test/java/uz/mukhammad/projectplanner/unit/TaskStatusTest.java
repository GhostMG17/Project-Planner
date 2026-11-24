package uz.mukhammad.projectplanner.unit;

import org.junit.jupiter.api.Test;
import uz.mukhammad.projectplanner.task.enums.TaskStatus;


import static org.assertj.core.api.Assertions.assertThat;

class TaskStatusTest {

    @Test
    void blockedTaskMustBeUnblockedBeforeCompletion() {
        assertThat(TaskStatus.BLOCKED.next()).doesNotContain(TaskStatus.COMPLETED);
    }

    @Test
    void completedTaskCanOnlyBeReopened() {
        assertThat(TaskStatus.COMPLETED.next()).containsExactly(TaskStatus.IN_PROGRESS);
    }

    @Test
    void newTaskCanGoAnywhereExceptNew() {
        assertThat(TaskStatus.NEW.next()).doesNotContain(TaskStatus.NEW).hasSize(4);
    }
}