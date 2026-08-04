package ticket_system.project.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ticket_system.project.service.TicketService;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class SlaScheduler {

    private final TicketService ticketService;

    @Scheduled(cron = "0 * * * * *")
    public void evaluateSla() {
        ticketService.evaluateSla();
    }
}
