package ro.tuc.ds2020.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String role   = request.getHeader("X-Role");
        String userId = request.getHeader("X-User-Id");

        if (role == null || role.isBlank() || userId == null || userId.isBlank()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing gateway headers");
            return false;
        }

        if ("ADMIN".equals(role)) {
            return true;
        }

        // Clients may only read; the ownership check is done in the controller.
        if ("CLIENT".equals(role) && "GET".equals(request.getMethod())) {
            return true;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
        return false;
    }
}
