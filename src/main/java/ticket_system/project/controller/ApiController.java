package ticket_system.project.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ticket_system.project.dto.AuthRequest;
import ticket_system.project.dto.CommentRequest;
import ticket_system.project.dto.RegistrationRequest;
import ticket_system.project.dto.TicketRequest;
import ticket_system.project.dto.TicketResponse;
import ticket_system.project.entity.TicketStatus;
import ticket_system.project.entity.User;
import ticket_system.project.service.AuthService;
import ticket_system.project.service.NotificationService;
import ticket_system.project.service.TicketService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final AuthService authService;
    private final TicketService ticketService;
    private final NotificationService notificationService;

    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationRequest request) {
        authService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/auth/login")
    public ResponseEntity<String> login(@Valid @RequestBody AuthRequest request) {
        authService.login(request);
        return ResponseEntity.ok("Login successful");
    }

    @PostMapping("/tickets")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketRequest request) {
        User customer = authService.findByEmail(getCurrentUserEmail());
        return ResponseEntity.ok(ticketService.createTicket(request, customer));
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<TicketResponse>> getTickets() {
        return ResponseEntity.ok(ticketService.findAll());
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.findById(id));
    }

    @PutMapping("/tickets/{id}/status")
    public ResponseEntity<TicketResponse> updateStatus(@PathVariable Long id, @RequestBody TicketStatus status) {
        User actor = authService.findByEmail(getCurrentUserEmail());
        return ResponseEntity.ok(ticketService.updateStatus(id, status, actor));
    }

    @PostMapping("/tickets/{id}/comment")
    public ResponseEntity<String> addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
        User user = authService.findByEmail(getCurrentUserEmail());
        ticketService.addComment(id, request, user);
        return ResponseEntity.ok("Comment added");
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> notifications() {
        User user = authService.findByEmail(getCurrentUserEmail());
        return ResponseEntity.ok(notificationService.getNotifications(user));
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
