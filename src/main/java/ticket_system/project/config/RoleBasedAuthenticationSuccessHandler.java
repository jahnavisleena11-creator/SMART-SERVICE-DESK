package ticket_system.project.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        boolean isAgent = authorities.stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPPORT_AGENT"));

        if (isAdmin) {
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
        } else if (isAgent) {
            response.sendRedirect(request.getContextPath() + "/agent/dashboard");
        } else {
            response.sendRedirect(request.getContextPath() + "/customer/dashboard");
        }
    }
}
