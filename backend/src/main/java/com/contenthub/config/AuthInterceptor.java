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

        if (path.startsWith("/api/auth/") || path.startsWith("/h2-console") || path.equals("/error") || path.startsWith("/api/debug")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}");
            return false;
        }

        String token = authHeader.substring(7);

        if (!jwtUtils.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid token\"}");
            return false;
        }

        Long userId = jwtUtils.getUserIdFromToken(token);
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"User not found\"}");
            return false;
        }

        request.setAttribute("userId", userId);
        return true;
    }
}
