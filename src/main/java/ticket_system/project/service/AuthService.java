package ticket_system.project.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ticket_system.project.dto.AuthRequest;
import ticket_system.project.dto.RegistrationRequest;
import ticket_system.project.entity.Role;
import ticket_system.project.entity.RoleName;
import ticket_system.project.entity.User;
import ticket_system.project.exception.UserNotFoundException;
import ticket_system.project.repository.RoleRepository;
import ticket_system.project.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public User register(RegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        RoleName roleName = request.getRole() == null ? RoleName.CUSTOMER : RoleName.valueOf(request.getRole().toUpperCase());
        Role role = roleRepository.findByName(roleName)
            .orElseGet(() -> {
                Role newRole = new Role();
                newRole.setName(roleName);
                return roleRepository.save(newRole);
            });

        user.setRoles(Set.of(role));
        return userRepository.save(user);
    }

    public Authentication login(AuthRequest request) {
        return authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }
}
