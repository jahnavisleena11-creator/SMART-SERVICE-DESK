package ticket_system.project.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import ticket_system.project.dto.TicketResponse;
import ticket_system.project.entity.User;
import ticket_system.project.service.AuthService;
import ticket_system.project.service.TicketService;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final AuthService authService;
    private final TicketService ticketService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User customer = authService.findByEmail(getCurrentUserEmail());
        List<TicketResponse> tickets = ticketService.findByCustomer(customer);
        model.addAttribute("tickets", tickets);
        return "customer-dashboard";
    }

    @GetMapping("/tickets/{id}")
    public String ticketDetails(@PathVariable Long id, Model model) {
        model.addAttribute("ticket", ticketService.findById(id));
        model.addAttribute("commentRequest", new ticket_system.project.dto.CommentRequest());
        return "ticket-details";
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
