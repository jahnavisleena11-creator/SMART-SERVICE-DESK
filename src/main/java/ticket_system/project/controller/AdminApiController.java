package ticket_system.project.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ticket_system.project.dto.AssignTicketRequest;
import ticket_system.project.dto.DashboardDTO;
import ticket_system.project.dto.RegistrationRequest;
import ticket_system.project.dto.SlaPolicyRequest;
import ticket_system.project.dto.UserDto;
import ticket_system.project.entity.Role;
import ticket_system.project.entity.RoleName;
import ticket_system.project.entity.SlaPolicy;
import ticket_system.project.entity.User;
import ticket_system.project.repository.RoleRepository;
import ticket_system.project.repository.SlaPolicyRepository;
import ticket_system.project.repository.UserRepository;
import ticket_system.project.service.TicketService;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final TicketService ticketService;

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList()));
    }

    @PostMapping("/users")
    public ResponseEntity<UserDto> createAgent(@RequestBody RegistrationRequest request) {
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        Role role = roleRepository.findByName(RoleName.SUPPORT_AGENT)
            .orElseGet(() -> roleRepository.save(new Role(null, RoleName.SUPPORT_AGENT)));
        user.setRoles(java.util.Set.of(role));
        return ResponseEntity.ok(toDto(userRepository.save(user)));
    }

    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<String> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.ok("Ticket deleted");
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard() {
        return ResponseEntity.ok(ticketService.buildDashboardDTO());
    }

    @GetMapping("/sla")
    public ResponseEntity<List<SlaPolicy>> getSlaPolicies() {
        return ResponseEntity.ok(slaPolicyRepository.findAll());
    }

    @PostMapping("/sla")
    public ResponseEntity<SlaPolicy> createSlaPolicy(@Valid @RequestBody SlaPolicyRequest request) {
        SlaPolicy policy = new SlaPolicy();
        policy.setPriority(request.getPriority());
        policy.setResponseHours(request.getResponseHours());
        policy.setResolutionHours(request.getResolutionHours());
        return ResponseEntity.ok(slaPolicyRepository.save(policy));
    }

    @PutMapping("/tickets/{id}/assign")
    public ResponseEntity<String> assignTicket(@PathVariable Long id, @Valid @RequestBody AssignTicketRequest request) {
        User agent = userRepository.findById(request.getAgentId()).orElseThrow();
        ticketService.assignTicket(id, agent);
        return ResponseEntity.ok("Ticket assigned");
    }

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRoles().stream().findFirst().map(role -> role.getName().name()).orElse("CUSTOMER"));
        return dto;
    }
}
