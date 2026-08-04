package ticket_system.project.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ticket_system.project.dto.CommentRequest;
import ticket_system.project.dto.TicketRequest;
import ticket_system.project.dto.TicketResponse;
import ticket_system.project.entity.User;
import ticket_system.project.entity.RoleName;
import ticket_system.project.repository.UserRepository;
import ticket_system.project.service.AuthService;
import ticket_system.project.service.TicketService;

@Controller
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final AuthService authService;
    private final UserRepository userRepository;

    @GetMapping("/new")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String createTicketPage(Model model) {
        model.addAttribute("ticketRequest", new TicketRequest());
        // Only customers can create tickets; hide create page for non-customers
        return "create-ticket";
    }

    @PostMapping("/new")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String createTicket(@Valid @ModelAttribute("ticketRequest") TicketRequest request) {
        User customer = authService.findByEmail(getCurrentUserEmail());
        ticketService.createTicket(request, customer);
        return "redirect:/customer/dashboard?success=true";
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public String createTicketFromRoot(@Valid @ModelAttribute("ticketRequest") TicketRequest request) {
        return createTicket(request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String myTickets(Model model, @RequestParam(required = false) String success) {
        User customer = authService.findByEmail(getCurrentUserEmail());
        List<TicketResponse> tickets = ticketService.findByCustomer(customer);
        model.addAttribute("tickets", tickets);
        model.addAttribute("success", success);
        return "my-tickets";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPPORT_AGENT', 'ADMIN')")
    public String ticketDetails(@PathVariable Long id, Model model) {
        model.addAttribute("ticket", ticketService.findById(id));
        model.addAttribute("auditEntries", ticketService.getAuditEntries(id));
        model.addAttribute("commentRequest", new ticket_system.project.dto.CommentRequest());

        User current = authService.findByEmail(getCurrentUserEmail());
        boolean isAdmin = current.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN);
        boolean isAgent = current.getRoles().stream().anyMatch(r -> r.getName() == RoleName.SUPPORT_AGENT);
        if (isAdmin) {
            model.addAttribute("agents", userRepository.findByRoles_Name(RoleName.SUPPORT_AGENT));
        } else if (isAgent) {
            model.addAttribute("agents", null);
        }

        return "ticket-details";
    }

    @PostMapping("/{id}/comment")
    public String addComment(@PathVariable Long id, @Valid @ModelAttribute("commentRequest") CommentRequest request) {
        User user = authService.findByEmail(getCurrentUserEmail());
        ticketService.addComment(id, request, user);
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'ADMIN')")
    public String assignTicket(@PathVariable Long id, @RequestParam(required = false) Long agentId) {
        User current = authService.findByEmail(getCurrentUserEmail());
        User toAssign = current;

        // if agentId provided and current user is admin, assign the selected agent
        boolean isAdmin = current.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN);
        if (agentId != null && agentId > 0 && isAdmin) {
            toAssign = userRepository.findById(agentId).orElse(current);
        }

        ticketService.assignTicket(id, toAssign);
        return "redirect:/tickets/" + id + "?success=assigned";
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'ADMIN')")
    public String updateStatus(@PathVariable Long id, @RequestParam String status, @RequestParam(required = false) String resolutionNotes) {
        User actor = authService.findByEmail(getCurrentUserEmail());
        ticketService.updateStatus(id, ticket_system.project.entity.TicketStatus.valueOf(status), actor, resolutionNotes);
        return "redirect:/tickets/" + id + "?success=updated";
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
