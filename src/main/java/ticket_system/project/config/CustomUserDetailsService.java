package ticket_system.project.config;

import java.util.Collection;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ticket_system.project.entity.Role;
import ticket_system.project.entity.User;
import ticket_system.project.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            user.isEnabled(),
            true,
            true,
            true,
            getAuthorities(user)
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        return user.getRoles().stream()
            .map(Role::getName)
            .flatMap(role -> {
                if (role == ticket_system.project.entity.RoleName.SUPPORT_AGENT) {
                    return java.util.stream.Stream.of(new SimpleGrantedAuthority("ROLE_SUPPORT_AGENT"));
                }
                if (role == ticket_system.project.entity.RoleName.CUSTOMER) {
                    return java.util.stream.Stream.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
                }
                return java.util.stream.Stream.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            })
            .collect(Collectors.toSet());
    }
}
