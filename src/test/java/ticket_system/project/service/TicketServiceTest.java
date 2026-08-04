package ticket_system.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import ticket_system.project.dto.DashboardDTO;
import ticket_system.project.dto.TicketRequest;
import ticket_system.project.dto.TicketResponse;
import ticket_system.project.entity.RoleName;
import ticket_system.project.entity.TicketPriority;
import ticket_system.project.entity.TicketStatus;
import ticket_system.project.entity.User;
import ticket_system.project.repository.CommentRepository;
import ticket_system.project.repository.NotificationRepository;
import ticket_system.project.repository.SlaPolicyRepository;
import ticket_system.project.repository.TicketRepository;
import ticket_system.project.repository.UserRepository;

class TicketServiceTest {

    @Test
    void createTicketStartsUnassignedAndOpen() {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        SlaPolicyRepository slaPolicyRepository = mock(SlaPolicyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        TicketService ticketService = new TicketService(
            ticketRepository,
            commentRepository,
            notificationRepository,
            slaPolicyRepository,
            userRepository
        );

        TicketRequest request = new TicketRequest();
        request.setTitle("Printer issue");
        request.setDescription("Printer offline");
        request.setPriority(TicketPriority.HIGH);
        request.setCategory("Hardware");

        User customer = new User();
        customer.setEmail("customer@example.com");
        customer.setFullName("Customer One");

        User agent = new User();
        agent.setId(12L);
        agent.setFullName("Agent One");

        when(slaPolicyRepository.findByPriority("HIGH")).thenReturn(Optional.empty());
        when(slaPolicyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByRoles_Name(RoleName.SUPPORT_AGENT)).thenReturn(List.of(agent));
        when(ticketRepository.save(any())).thenAnswer(invocation -> {
            ticket_system.project.entity.Ticket ticket = invocation.getArgument(0);
            ticket.setId(1L);
            return ticket;
        });

        TicketResponse response = ticketService.createTicket(request, customer);

        assertEquals(TicketStatus.OPEN, response.getStatus());
        assertNull(response.getAssignedAgentName());
    }

    @Test
    void buildDashboardAggregatesCountsAndTrends() {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        SlaPolicyRepository slaPolicyRepository = mock(SlaPolicyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        TicketService ticketService = new TicketService(
            ticketRepository,
            commentRepository,
            notificationRepository,
            slaPolicyRepository,
            userRepository
        );

        when(ticketRepository.count()).thenReturn(4L);
        when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.RESOLVED)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.CLOSED)).thenReturn(0L);
        when(ticketRepository.countBySlaStatus(ticket_system.project.entity.SlaStatus.BREACHED)).thenReturn(1L);
        when(ticketRepository.countByPriority(TicketPriority.HIGH)).thenReturn(2L);
        when(ticketRepository.countByPriority(TicketPriority.MEDIUM)).thenReturn(1L);
        when(ticketRepository.countByPriority(TicketPriority.LOW)).thenReturn(1L);
        when(ticketRepository.countByPriority(TicketPriority.CRITICAL)).thenReturn(0L);
        when(userRepository.findByRoles_Name(RoleName.SUPPORT_AGENT)).thenReturn(List.of(new User()));
        when(userRepository.countByRoles_Name(RoleName.CUSTOMER)).thenReturn(2L);

        DashboardDTO dashboard = ticketService.buildDashboardDTO();

        assertEquals(4L, dashboard.getTotalTickets());
        assertEquals(2L, dashboard.getOpenTickets());
        assertEquals(1L, dashboard.getInProgressTickets());
        assertEquals(1L, dashboard.getResolvedTickets());
        assertEquals(0L, dashboard.getClosedTickets());
        assertEquals(1L, dashboard.getBreachedSlaTickets());
    }
}
