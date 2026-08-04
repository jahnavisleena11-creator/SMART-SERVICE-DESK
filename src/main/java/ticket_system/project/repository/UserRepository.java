package ticket_system.project.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ticket_system.project.entity.RoleName;
import ticket_system.project.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Find users by role name (e.g., SUPPORT_AGENT)
    List<User> findByRoles_Name(RoleName name);

    long countByRoles_Name(RoleName name);
}
