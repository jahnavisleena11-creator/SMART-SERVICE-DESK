package ticket_system.project.dto;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardDTO {
    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long resolvedTickets;
    private long closedTickets;
    private long breachedSlaTickets;
    private long activeAgents;
    private long activeCustomers;
    private Map<String, Long> ticketsByPriority;
    private Map<String, Long> ticketsByStatus;
    private List<Map<String, Object>> createdTicketsLast7Days;
    private Map<String, Long> slaCompliance;
    private double averageResolutionTimeHours;
    private double averageResponseTimeHours;
    private double slaCompliancePercentage;
    private double averageTicketsPerAgent;
    private List<Map<String, Object>> recentActivity;
    private List<Map<String, Object>> topAgents;
}
