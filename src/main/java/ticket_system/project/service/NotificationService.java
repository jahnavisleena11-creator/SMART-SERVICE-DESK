package ticket_system.project.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ticket_system.project.entity.Notification;
import ticket_system.project.entity.User;
import ticket_system.project.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getNotifications(User user) {
        return notificationRepository.findByReceiverOrderByCreatedAtDesc(user);
    }

    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setReadStatus(true);
            notificationRepository.save(notification);
        });
    }
}
