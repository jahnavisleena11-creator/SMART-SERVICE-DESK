package ticket_system.project.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import ticket_system.project.entity.SlaStatus;
import ticket_system.project.entity.TicketPriority;
import ticket_system.project.entity.TicketStatus;

@Getter
@Setter
public class TicketResponse {
    private Long id;
    private String title;
    private String description;
    private TicketPriority priority;
    private String category;
    private String customerName;
    private String assignedAgentName;
    private TicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime responseDeadline;
    private LocalDateTime resolutionDeadline;
    private String resolutionNotes;
    private String remainingSlaText;
    private LocalDateTime closedAt;
    private SlaStatus slaStatus;
    private boolean responseSlaBreached;
    private boolean resolutionSlaBreached;
}
