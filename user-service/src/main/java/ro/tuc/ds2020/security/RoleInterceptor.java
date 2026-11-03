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

        if (role == null || role.isBlank()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing gateway headers");
            return false;
        }

        if ("ADMIN".equals(role)) {
            return true;
        }

        if ("CLIENT".equals(role)) {

            String method = request.getMethod();
            String path   = request.getRequestURI();

            if ("GET".equals(method) && path.startsWith("/user/") && path.length() > 6) {
                String pathUserId = path.substring("/user/".length()).replaceAll("/$", "");
                if (pathUserId.equals(userId)) {
                    return true;
                }
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Clients can only view their own profile");
            return false;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unknown role");
        return false;
    }
}
