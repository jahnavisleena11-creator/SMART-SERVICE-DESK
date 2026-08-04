package ticket_system.project.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;
import ticket_system.project.entity.SlaStatus;
import ticket_system.project.entity.Ticket;
import ticket_system.project.entity.TicketPriority;
import ticket_system.project.entity.TicketStatus;
import ticket_system.project.entity.User;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByCustomer(User customer);

    List<Ticket> findByAssignedAgent(User assignedAgent);

    List<Ticket> findByStatus(TicketStatus status);

    long countByStatus(TicketStatus status);

    long countBySlaStatus(SlaStatus slaStatus);

    long countByPriority(TicketPriority priority);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT t FROM Ticket t WHERE t.status != 'CLOSED' AND t.responseDeadline < ?1")
    List<Ticket> findOpenTicketsWithExpiredResponseDeadline(LocalDateTime now);

    @Query("SELECT t FROM Ticket t WHERE t.status != 'CLOSED' AND t.resolutionDeadline < ?1")
    List<Ticket> findOpenTicketsWithExpiredResolutionDeadline(LocalDateTime now);
}
