package ticket_system.project.dto;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardSummary {
    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long resolvedTickets;
    private long closedTickets;
    private long slaBreachedTickets;
    private Map<String, Long> ticketsByPriority;
    private Map<String, Long> ticketsByStatus;
    private Map<String, Long> ticketsByCategory;
    private double averageResolutionTimeHours;
    private double averageResponseTimeHours;
    private double slaCompliancePercentage;
    private double averageTicketsPerAgent;
    private Map<String, Long> agentPerformance;
    private List<Map<String, Object>> recentActivity;
    private List<Map<String, Object>> topAgents;
    private List<Map<String, Object>> createdTicketsLast7Days;
}
