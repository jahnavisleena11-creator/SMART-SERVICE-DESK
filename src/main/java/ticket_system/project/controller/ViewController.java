package ticket_system.project.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ticket_system.project.dto.TicketResponse;
import ticket_system.project.service.TicketService;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final TicketService ticketService;

    // Agent dashboard is handled by AgentController to avoid duplicate mappings

    @GetMapping("/notifications")
    public String notifications() {
        return "notifications";
    }

    @GetMapping("/tickets/list")
    public String ticketList(Model model) {
        List<TicketResponse> tickets = ticketService.findAll();
        model.addAttribute("tickets", tickets);
        return "ticket-list";
    }
}
