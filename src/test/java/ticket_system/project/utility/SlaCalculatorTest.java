package ticket_system.project.utility;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SlaCalculatorTest {

    @Test
    void shouldCalculateDeadlineByAddingHours() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 21, 10, 0);

        LocalDateTime responseDeadline = SlaCalculator.calculateDeadline(createdAt, 8);

        assertThat(responseDeadline).isEqualTo(createdAt.plusHours(8));
    }
}
