package ticket_system.project.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ticket_system.project.entity.SlaPolicy;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {
    Optional<SlaPolicy> findByPriority(String priority);
}
