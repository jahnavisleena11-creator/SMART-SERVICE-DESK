package ticket_system.project.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ticket_system.project.dto.TicketRequest;
import ticket_system.project.dto.TicketResponse;
import ticket_system.project.entity.User;
import ticket_system.project.repository.UserRepository;
import ticket_system.project.service.AuthService;
import ticket_system.project.service.TicketService;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    @Mock
    private TicketService ticketService;

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketController ticketController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTicketRedirectsToCustomerDashboard() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("customer@example.com", "password"));

        User customer = new User();
        customer.setEmail("customer@example.com");

        when(authService.findByEmail("customer@example.com")).thenReturn(customer);
        when(ticketService.createTicket(any(TicketRequest.class), eq(customer))).thenReturn(new TicketResponse());

        String viewName = ticketController.createTicket(new TicketRequest());

        assertEquals("redirect:/customer/dashboard?success=true", viewName);
    }
}
