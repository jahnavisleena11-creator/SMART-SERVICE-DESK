package ticket_system.project.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ticket_system.project.dto.DashboardSummary;
import ticket_system.project.dto.RegistrationRequest;
import ticket_system.project.entity.Role;
import ticket_system.project.entity.RoleName;
import ticket_system.project.entity.TicketStatus;
import ticket_system.project.entity.User;
import ticket_system.project.repository.RoleRepository;
import ticket_system.project.repository.UserRepository;
import ticket_system.project.service.AuthService;
import ticket_system.project.service.TicketService;
import ticket_system.project.service.UserService;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TicketService ticketService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String dashboard(Model model) {
        DashboardSummary summary = ticketService.getDashboardSummary();
        model.addAttribute("summary", summary);
        model.addAttribute("recentTickets", ticketService.findAll().stream().limit(5).toList());
        model.addAttribute("tickets", ticketService.findAll());
        model.addAttribute("recentNotifications", java.util.List.of());
        model.addAttribute("agents", getSupportAgents());
        return "admin-dashboard";
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String users(Model model) {
        model.addAttribute("users", userService.getCustomers());
        return "manage-users";
    }

    @PostMapping("/users/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean deleted = userService.deleteUser(id);
        if (!deleted) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
        } else {
            redirectAttributes.addFlashAttribute("success", "User removed successfully.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean updated = userService.toggleUserStatus(id);
        if (!updated) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
        } else {
            redirectAttributes.addFlashAttribute("success", "User status updated.");
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/agents")
    @PreAuthorize("hasRole('ADMIN')")
    public String agents(Model model) {
        List<User> supportAgents = getSupportAgents();
        model.addAttribute("agents", supportAgents);
        return "manage-agents";
    }

    @PostMapping("/tickets/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public String assignTicket(@PathVariable Long id, @RequestParam(required = false) Long agentId, RedirectAttributes redirectAttributes) {
        User currentAdmin = authService.findByEmail(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());

        if (agentId != null && agentId > 0) {
            User targetAgent = userRepository.findById(agentId).orElse(null);
            if (targetAgent == null || !isSupportAgent(targetAgent)) {
                redirectAttributes.addFlashAttribute("error", "Selected agent could not be found.");
                return "redirect:/admin/dashboard";
            }
            ticketService.assignTicket(id, targetAgent);
            ticketService.updateStatus(id, TicketStatus.ASSIGNED, currentAdmin);
            redirectAttributes.addFlashAttribute("success", "Ticket assigned successfully.");
            return "redirect:/admin/dashboard";
        }

        redirectAttributes.addFlashAttribute("error", "Please select an agent.");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/agents/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createAgent(@RequestParam String fullName, @RequestParam String email, @RequestParam String password, RedirectAttributes redirectAttributes) {
        RegistrationRequest request = new RegistrationRequest();
        request.setFullName(fullName);
        request.setEmail(email);
        request.setPassword(password);
        request.setRole("SUPPORT_AGENT");
        authService.register(request);
        redirectAttributes.addFlashAttribute("success", "Agent created successfully.");
        return "redirect:/admin/agents";
    }

    @PostMapping("/agents/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleAgentStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User agent = userRepository.findById(id).orElse(null);
        if (agent == null || !isSupportAgent(agent)) {
            redirectAttributes.addFlashAttribute("error", "Agent not found.");
            return "redirect:/admin/agents";
        }
        agent.setEnabled(!agent.isEnabled());
        userRepository.save(agent);
        redirectAttributes.addFlashAttribute("success", "Agent status updated.");
        return "redirect:/admin/agents";
    }

    @PostMapping("/agents/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteAgent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User agent = userRepository.findById(id).orElse(null);
        if (agent == null || !isSupportAgent(agent)) {
            redirectAttributes.addFlashAttribute("error", "Agent not found.");
            return "redirect:/admin/agents";
        }
        agent.setEnabled(false);
        userRepository.save(agent);
        redirectAttributes.addFlashAttribute("success", "Agent disabled successfully.");
        return "redirect:/admin/agents";
    }

    @GetMapping("/sla")
    @PreAuthorize("hasRole('ADMIN')")
    public String sla(Model model) {
        List<Role> roles = roleRepository.findAll();
        model.addAttribute("roles", roles);
        return "manage-sla";
    }

    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public String reports(Model model) {
        DashboardSummary summary = ticketService.getDashboardSummary();
        model.addAttribute("summary", summary);
        model.addAttribute("dashboard", ticketService.buildDashboardDTO());
        model.addAttribute("statusLabels", java.util.List.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"));
        model.addAttribute("statusValues", java.util.List.of(
            summary.getOpenTickets(),
            summary.getInProgressTickets(),
            summary.getResolvedTickets(),
            summary.getClosedTickets()
        ));
        model.addAttribute("slaLabels", java.util.List.of("On Time", "Breached"));
        model.addAttribute("slaValues", java.util.List.of(
            summary.getTotalTickets() - summary.getSlaBreachedTickets(),
            summary.getSlaBreachedTickets()
        ));
        model.addAttribute("agents", getSupportAgents());
        return "reports";
    }

    private java.util.List<User> getSupportAgents() {
        return userRepository.findAll().stream()
            .filter(this::isSupportAgent)
            .toList();
    }

    private boolean isSupportAgent(User user) {
        return user != null && user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.SUPPORT_AGENT);
    }
}
