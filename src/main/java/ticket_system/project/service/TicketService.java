package ticket_system.project.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ticket_system.project.dto.CommentRequest;
import ticket_system.project.dto.DashboardDTO;
import ticket_system.project.dto.DashboardSummary;
import ticket_system.project.dto.TicketRequest;
import ticket_system.project.dto.TicketResponse;
import ticket_system.project.entity.Comment;
import ticket_system.project.entity.Notification;
import ticket_system.project.entity.RoleName;
import ticket_system.project.entity.SlaPolicy;
import ticket_system.project.entity.SlaStatus;
import ticket_system.project.entity.Ticket;
import ticket_system.project.entity.TicketPriority;
import ticket_system.project.entity.TicketStatus;
import ticket_system.project.entity.User;
import ticket_system.project.exception.InvalidStatusException;
import ticket_system.project.exception.TicketNotFoundException;
import ticket_system.project.repository.CommentRepository;
import ticket_system.project.repository.NotificationRepository;
import ticket_system.project.repository.SlaPolicyRepository;
import ticket_system.project.repository.TicketRepository;
import ticket_system.project.utility.SlaCalculator;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final ticket_system.project.repository.UserRepository userRepository;

    // simple in-memory round-robin counter for agent assignment
    private static final AtomicInteger rrCounter = new AtomicInteger(0);

    public TicketResponse createTicket(TicketRequest request, User customer) {
        SlaPolicy policy = slaPolicyRepository.findByPriority(request.getPriority().name())
            .orElseGet(() -> createDefaultPolicy(request.getPriority()));

        Ticket ticket = new Ticket();
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority());
        ticket.setCategory(request.getCategory());
        ticket.setCustomer(customer);
        ticket.setAssignedAgent(null);
        ticket.setStatus(TicketStatus.OPEN);
        LocalDateTime now = LocalDateTime.now();
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        ticket.setResponseDeadline(SlaCalculator.calculateDeadline(now, policy.getResponseHours()));
        ticket.setResolutionDeadline(SlaCalculator.calculateDeadline(now, policy.getResolutionHours()));
        ticket.setSlaStatus(SlaStatus.ON_TIME);

        Ticket saved = ticketRepository.save(ticket);
        notifyUser(customer, "Your ticket '" + saved.getTitle() + "' has been created.");
        if (saved.getAssignedAgent() != null) {
            notifyUser(saved.getAssignedAgent(), "You have been assigned ticket #" + saved.getId());
        }
        return mapToResponse(saved);
    }

    public List<TicketResponse> findAll() {
        return ticketRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public List<TicketResponse> findByCustomer(User customer) {
        return ticketRepository.findByCustomer(customer).stream().map(this::mapToResponse).toList();
    }

    public List<TicketResponse> findByAgent(User agent) {
        return ticketRepository.findByAssignedAgent(agent).stream().map(this::mapToResponse).toList();
    }

    public TicketResponse findById(Long id) {
        return mapToResponse(getTicket(id));
    }

    public TicketResponse assignTicket(Long id, User agent) {
        Ticket ticket = getTicket(id);
        ticket.setAssignedAgent(agent);
        ticket.setStatus(TicketStatus.ASSIGNED);
        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);
        notifyUser(agent, "You have been assigned ticket #" + saved.getId());
        notifyUser(ticket.getCustomer(), "Ticket #" + saved.getId() + " has been assigned to an agent.");
        return mapToResponse(saved);
    }

    public TicketResponse updateStatus(Long id, TicketStatus status, User actor) {
        return updateStatus(id, status, actor, null);
    }

    public TicketResponse updateStatus(Long id, TicketStatus status, User actor, String resolutionNotes) {
        Ticket ticket = getTicket(id);
        if (status == TicketStatus.CLOSED && ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new InvalidStatusException("Ticket must be resolved before it can be closed");
        }
        if (resolutionNotes != null && !resolutionNotes.isBlank()) {
            ticket.setResolutionNotes(resolutionNotes);
        }
        ticket.setStatus(status);
        if (status == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);
        notifyUser(ticket.getCustomer(), "Ticket #" + saved.getId() + " status changed to " + saved.getStatus());
        notifyUser(actor, "You updated ticket #" + saved.getId() + " to " + saved.getStatus());
        return mapToResponse(saved);
    }

    public TicketResponse resolveTicket(Long id, User actor) {
        Ticket ticket = getTicket(id);
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);
        notifyUser(ticket.getCustomer(), "Ticket #" + saved.getId() + " has been resolved.");
        notifyUser(actor, "You resolved ticket #" + saved.getId());
        return mapToResponse(saved);
    }

    public void deleteTicket(Long id) {
        Ticket ticket = getTicket(id);
        ticketRepository.delete(ticket);
    }

    public Comment addComment(Long id, CommentRequest request, User user) {
        Ticket ticket = getTicket(id);
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setUser(user);
        comment.setMessage(request.getMessage());
        return commentRepository.save(comment);
    }

    public DashboardSummary getDashboardSummary() {
        DashboardSummary summary = new DashboardSummary();
        DashboardDTO dashboard = buildDashboardDTO();
        summary.setTotalTickets(dashboard.getTotalTickets());
        summary.setOpenTickets(dashboard.getOpenTickets());
        summary.setInProgressTickets(dashboard.getInProgressTickets());
        summary.setResolvedTickets(dashboard.getResolvedTickets());
        summary.setClosedTickets(dashboard.getClosedTickets());
        summary.setSlaBreachedTickets(dashboard.getBreachedSlaTickets());
        summary.setTicketsByPriority(dashboard.getTicketsByPriority());
        summary.setTicketsByStatus(dashboard.getTicketsByStatus());
        summary.setAverageResolutionTimeHours(dashboard.getAverageResolutionTimeHours());
        summary.setAverageResponseTimeHours(dashboard.getAverageResponseTimeHours());
        summary.setSlaCompliancePercentage(dashboard.getSlaCompliancePercentage());
        summary.setAverageTicketsPerAgent(dashboard.getAverageTicketsPerAgent());
        summary.setRecentActivity(dashboard.getRecentActivity());
        summary.setTopAgents(dashboard.getTopAgents());
        summary.setCreatedTicketsLast7Days(dashboard.getCreatedTicketsLast7Days());
        return summary;
    }

    public DashboardDTO buildDashboardDTO() {
        DashboardDTO dashboard = new DashboardDTO();
        long totalTickets = ticketRepository.count();
        long openTickets = ticketRepository.countByStatus(TicketStatus.OPEN) + ticketRepository.countByStatus(TicketStatus.ASSIGNED);
        long inProgressTickets = ticketRepository.countByStatus(TicketStatus.IN_PROGRESS);
        long resolvedTickets = ticketRepository.countByStatus(TicketStatus.RESOLVED);
        long closedTickets = ticketRepository.countByStatus(TicketStatus.CLOSED);
        long breachedSlaTickets = ticketRepository.countBySlaStatus(ticket_system.project.entity.SlaStatus.BREACHED);
        long activeAgents = userRepository.findByRoles_Name(RoleName.SUPPORT_AGENT).stream().filter(User::isEnabled).count();
        long activeCustomers = userRepository.countByRoles_Name(RoleName.CUSTOMER);

        dashboard.setTotalTickets(totalTickets);
        dashboard.setOpenTickets(openTickets);
        dashboard.setInProgressTickets(inProgressTickets);
        dashboard.setResolvedTickets(resolvedTickets);
        dashboard.setClosedTickets(closedTickets);
        dashboard.setBreachedSlaTickets(breachedSlaTickets);
        dashboard.setActiveAgents(activeAgents);
        dashboard.setActiveCustomers(activeCustomers);
        dashboard.setTicketsByPriority(Map.of(
            "HIGH", ticketRepository.countByPriority(TicketPriority.HIGH),
            "MEDIUM", ticketRepository.countByPriority(TicketPriority.MEDIUM),
            "LOW", ticketRepository.countByPriority(TicketPriority.LOW),
            "CRITICAL", ticketRepository.countByPriority(TicketPriority.CRITICAL)
        ));
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        statusCounts.put("OPEN", ticketRepository.countByStatus(TicketStatus.OPEN));
        statusCounts.put("ASSIGNED", ticketRepository.countByStatus(TicketStatus.ASSIGNED));
        statusCounts.put("IN_PROGRESS", ticketRepository.countByStatus(TicketStatus.IN_PROGRESS));
        statusCounts.put("RESOLVED", ticketRepository.countByStatus(TicketStatus.RESOLVED));
        statusCounts.put("CLOSED", ticketRepository.countByStatus(TicketStatus.CLOSED));
        dashboard.setTicketsByStatus(statusCounts);
        dashboard.setCreatedTicketsLast7Days(buildCreatedTicketsLast7Days());
        dashboard.setSlaCompliance(Map.of(
            "ON_TIME", ticketRepository.countBySlaStatus(ticket_system.project.entity.SlaStatus.ON_TIME),
            "BREACHED", breachedSlaTickets
        ));
        dashboard.setAverageResolutionTimeHours(calculateAverageResolutionHours());
        dashboard.setAverageResponseTimeHours(calculateAverageResponseTimeHours());
        dashboard.setSlaCompliancePercentage(calculateSlaCompliancePercentage());
        dashboard.setAverageTicketsPerAgent(calculateAverageTicketsPerAgent(activeAgents));
        dashboard.setRecentActivity(buildRecentActivity());
        dashboard.setTopAgents(buildTopAgents());
        return dashboard;
    }

    public List<Map<String, Object>> getAuditEntries(Long ticketId) {
        Ticket ticket = getTicket(ticketId);
        List<Map<String, Object>> entries = new ArrayList<>();
        entries.add(Map.of("label", "Created", "value", ticket.getCreatedAt().toString()));
        entries.add(Map.of("label", "Updated", "value", ticket.getUpdatedAt().toString()));
        entries.add(Map.of("label", "Status", "value", ticket.getStatus().name()));
        if (ticket.getAssignedAgent() != null) {
            entries.add(Map.of("label", "Assigned Agent", "value", ticket.getAssignedAgent().getFullName()));
        }
        return entries;
    }

    public void evaluateSla() {
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> tickets = ticketRepository.findAll();
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.RESOLVED) {
                continue;
            }
            boolean responseBreached = ticket.getResponseDeadline() != null && now.isAfter(ticket.getResponseDeadline());
            boolean resolutionBreached = ticket.getResolutionDeadline() != null && now.isAfter(ticket.getResolutionDeadline());
            ticket.setResponseSlaBreached(responseBreached);
            ticket.setResolutionSlaBreached(resolutionBreached);

            // Simple near-breach thresholds: response within 60 minutes, resolution within 240 minutes
            boolean responseNear = ticket.getResponseDeadline() != null && now.isBefore(ticket.getResponseDeadline()) && java.time.Duration.between(now, ticket.getResponseDeadline()).toMinutes() <= 60;
            boolean resolutionNear = ticket.getResolutionDeadline() != null && now.isBefore(ticket.getResolutionDeadline()) && java.time.Duration.between(now, ticket.getResolutionDeadline()).toMinutes() <= 240;

            if (responseBreached || resolutionBreached) {
                ticket.setSlaStatus(SlaStatus.BREACHED);
            } else if (responseNear || resolutionNear) {
                ticket.setSlaStatus(SlaStatus.NEAR_BREACH);
            } else {
                ticket.setSlaStatus(SlaStatus.ON_TIME);
            }
            if (ticket.getSlaStatus() == SlaStatus.BREACHED) {
                notifyUser(ticket.getCustomer(), "SLA breach detected for ticket #" + ticket.getId());
                if (ticket.getAssignedAgent() != null) {
                    notifyUser(ticket.getAssignedAgent(), "SLA breach detected for ticket #" + ticket.getId());
                }
            }
            ticket.setUpdatedAt(now);
            ticketRepository.save(ticket);
        }
    }

    private SlaPolicy createDefaultPolicy(TicketPriority priority) {
        SlaPolicy policy = new SlaPolicy();
        policy.setPriority(priority.name());
        policy.setResponseHours(getDefaultResponseHours(priority));
        policy.setResolutionHours(getDefaultResolutionHours(priority));
        return slaPolicyRepository.save(policy);
    }

    private int getDefaultResponseHours(TicketPriority priority) {
        return switch (priority) {
            case LOW -> 8;
            case MEDIUM -> 4;
            case HIGH -> 2;
            case CRITICAL -> 1;
        };
    }

    private int getDefaultResolutionHours(TicketPriority priority) {
        return switch (priority) {
            case LOW -> 72;
            case MEDIUM -> 48;
            case HIGH -> 8;
            case CRITICAL -> 4;
        };
    }

    private Ticket getTicket(Long id) {
        return ticketRepository.findById(id)
            .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
    }

    private void notifyUser(User receiver, String message) {
        Notification notification = new Notification();
        notification.setReceiver(receiver);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        response.setId(ticket.getId());
        response.setTitle(ticket.getTitle());
        response.setDescription(ticket.getDescription());
        response.setPriority(ticket.getPriority());
        response.setCategory(ticket.getCategory());
        response.setCustomerName(ticket.getCustomer() != null ? ticket.getCustomer().getFullName() : null);
        response.setAssignedAgentName(ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getFullName() : null);
        response.setStatus(ticket.getStatus());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());
        response.setResponseDeadline(ticket.getResponseDeadline());
        response.setResolutionDeadline(ticket.getResolutionDeadline());
        response.setResolutionNotes(ticket.getResolutionNotes());
        response.setRemainingSlaText(calculateRemainingSlaText(ticket));
        response.setClosedAt(ticket.getClosedAt());
        response.setSlaStatus(ticket.getSlaStatus());
        response.setResponseSlaBreached(ticket.isResponseSlaBreached());
        response.setResolutionSlaBreached(ticket.isResolutionSlaBreached());
        return response;
    }

    private String calculateRemainingSlaText(Ticket ticket) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = ticket.getResolutionDeadline();
        if (deadline == null) {
            return "N/A";
        }
        Duration remaining = Duration.between(now, deadline);
        if (remaining.isNegative()) {
            return "Expired";
        }
        long hours = remaining.toHours();
        long minutes = remaining.toMinutesPart();
        return hours + "h " + minutes + "m remaining";
    }

    private double calculateAverageResolutionHours() {
        List<Ticket> resolved = ticketRepository.findAll().stream()
            .filter(ticket -> ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.RESOLVED)
            .toList();
        if (resolved.isEmpty()) {
            return 0;
        }
        double total = resolved.stream()
            .mapToLong(ticket -> Duration.between(ticket.getCreatedAt(), ticket.getClosedAt() != null ? ticket.getClosedAt() : LocalDateTime.now()).toHours())
            .sum();
        return total / resolved.size();
    }

    private double calculateAverageResponseTimeHours() {
        List<Ticket> tickets = ticketRepository.findAll();
        List<Ticket> withResponse = tickets.stream().filter(ticket -> ticket.getUpdatedAt() != null && ticket.getCreatedAt() != null).toList();
        if (withResponse.isEmpty()) {
            return 0;
        }
        double total = withResponse.stream()
            .mapToLong(ticket -> Duration.between(ticket.getCreatedAt(), ticket.getUpdatedAt()).toHours())
            .sum();
        return total / withResponse.size();
    }

    private double calculateSlaCompliancePercentage() {
        long total = ticketRepository.count();
        if (total == 0) {
            return 0;
        }
        long onTime = ticketRepository.countBySlaStatus(ticket_system.project.entity.SlaStatus.ON_TIME);
        return Math.round((onTime * 100.0 / total) * 100.0) / 100.0;
    }

    private double calculateAverageTicketsPerAgent(long activeAgents) {
        if (activeAgents == 0) {
            return 0;
        }
        return Math.round((ticketRepository.count() * 1.0 / activeAgents) * 100.0) / 100.0;
    }

    private List<Map<String, Object>> buildCreatedTicketsLast7Days() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();
            long count = ticketRepository.countByCreatedAtBetween(start, end);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("day", day.toString());
            entry.put("count", count);
            result.add(entry);
        }
        return result;
    }

    private List<Map<String, Object>> buildRecentActivity() {
        var page = ticketRepository.findAll(PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "updatedAt")));
        return (page != null ? page.getContent() : ticketRepository.findAll().stream().sorted(Comparator.comparing(Ticket::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).limit(8).toList())
            .stream()
            .map(ticket -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", ticket.getId());
                entry.put("customer", ticket.getCustomer() != null ? ticket.getCustomer().getFullName() : "Unassigned");
                entry.put("agent", ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getFullName() : "Unassigned");
                entry.put("status", ticket.getStatus().name());
                entry.put("priority", ticket.getPriority().name());
                entry.put("updatedAt", ticket.getUpdatedAt());
                return entry;
            })
            .toList();
    }

    private List<Map<String, Object>> buildTopAgents() {
        return userRepository.findByRoles_Name(RoleName.SUPPORT_AGENT).stream()
            .filter(User::isEnabled)
            .map(agent -> {
                long resolved = ticketRepository.findAll().stream()
                    .filter(ticket -> ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getId().equals(agent.getId()) && (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED))
                    .count();
                long total = ticketRepository.findAll().stream()
                    .filter(ticket -> ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getId().equals(agent.getId()))
                    .count();
                double success = total == 0 ? 0 : Math.round((resolved * 100.0 / total) * 100.0) / 100.0;
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("name", agent.getFullName());
                entry.put("resolvedTickets", resolved);
                entry.put("averageResolutionTimeHours", calculateAverageResolutionForAgent(agent));
                entry.put("slaSuccess", success);
                return entry;
            })
            .sorted(Comparator.comparing(entry -> (Long) entry.get("resolvedTickets"), Comparator.reverseOrder()))
            .limit(5)
            .toList();
    }

    private double calculateAverageResolutionForAgent(User agent) {
        List<Ticket> resolved = ticketRepository.findAll().stream()
            .filter(ticket -> ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getId().equals(agent.getId()) && (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED))
            .toList();
        if (resolved.isEmpty()) {
            return 0;
        }
        double total = resolved.stream()
            .mapToLong(ticket -> Duration.between(ticket.getCreatedAt(), ticket.getClosedAt() != null ? ticket.getClosedAt() : LocalDateTime.now()).toHours())
            .sum();
        return Math.round((total / resolved.size()) * 100.0) / 100.0;
    }

    private Map<String, Long> buildAgentPerformance(List<Ticket> tickets) {
        return tickets.stream()
            .filter(ticket -> ticket.getAssignedAgent() != null)
            .collect(Collectors.groupingBy(ticket -> ticket.getAssignedAgent().getFullName(), Collectors.counting()));
    }
}
