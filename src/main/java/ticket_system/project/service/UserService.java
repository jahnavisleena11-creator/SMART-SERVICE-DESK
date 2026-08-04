package ticket_system.project.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ticket_system.project.entity.RoleName;
import ticket_system.project.entity.User;
import ticket_system.project.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> getCustomers() {
        return userRepository.findByRoles_Name(RoleName.CUSTOMER);
    }

    public boolean deleteUser(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return false;
        }
        userRepository.delete(user);
        return true;
    }

    public boolean toggleUserStatus(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return false;
        }
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return true;
    }
}
