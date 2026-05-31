package com.contenthub.config;

import com.contenthub.common.JwtUtils;
import com.contenthub.entity.User;
import com.contenthub.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        if (path.startsWith("/api/auth/") || path.equals("/error") || path.startsWith("/api/debug")) {
            return true;
        }

        if (path.equals("/h2-console") || path.startsWith("/h2-console")) {
            response.sendRedirect("/");
            return false;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "Unauthorized: missing or invalid Authorization header");
            return false;
        }

        String token = authHeader.substring(7);

        if (!jwtUtils.validateToken(token)) {
            sendUnauthorized(response, "Unauthorized: invalid or expired token");
            return false;
        }

        Long userId = jwtUtils.getUserIdFromToken(token);
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            sendUnauthorized(response, "Unauthorized: user not found");
            return false;
        }

        request.setAttribute("userId", userId);
        return true;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
        response.getWriter().flush();
    }
}
