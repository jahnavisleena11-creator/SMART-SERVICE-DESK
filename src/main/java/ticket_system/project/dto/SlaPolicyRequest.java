package ticket_system.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SlaPolicyRequest {
    @NotBlank
    private String priority;

    @NotNull
    private Integer responseHours;

    @NotNull
    private Integer resolutionHours;
}
