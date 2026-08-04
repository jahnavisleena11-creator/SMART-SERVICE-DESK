package ticket_system.project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchFilterRequest {
    private String search;
    private String status;
    private String priority;
    private String category;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}
