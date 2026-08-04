package ticket_system.project.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ticket_system.project.entity.Comment;
import ticket_system.project.entity.Ticket;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}
