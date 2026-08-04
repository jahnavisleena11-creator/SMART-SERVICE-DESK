package ticket_system.project.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ticket_system.project.entity.Role;
import ticket_system.project.entity.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
