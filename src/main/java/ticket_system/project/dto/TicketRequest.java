package ticket_system.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import ticket_system.project.entity.TicketPriority;

@Getter
@Setter
public class TicketRequest {
    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String description;

    private TicketPriority priority;

    @NotBlank
    private String category;
}
