package ticket_system.project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
    private Long id;
    private String fullName;
    private String email;
    private String role;
}
