package ticket_system.project.utility;

import java.time.LocalDateTime;

public final class SlaCalculator {

    private SlaCalculator() {}

    public static LocalDateTime calculateDeadline(LocalDateTime createdAt, int hours) {
        return createdAt.plusHours(hours);
    }
}
