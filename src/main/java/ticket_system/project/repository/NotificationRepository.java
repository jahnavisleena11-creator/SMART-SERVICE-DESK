package ticket_system.project.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ticket_system.project.entity.Notification;
import ticket_system.project.entity.User;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverOrderByCreatedAtDesc(User receiver);
}
