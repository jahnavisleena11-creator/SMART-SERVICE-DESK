package ticket_system.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignTicketRequest {
    @NotNull
    private Long agentId;
}
