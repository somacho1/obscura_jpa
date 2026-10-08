package dev.jpa.obscura_jpa.auth;

import java.io.IOException;
import java.util.Set;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiRequestFilter extends OncePerRequestFilter {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 쿠키 인증 요청을 외부 사이트의 일반 폼으로 실행하지 못하도록 합니다.
        // 허용 Origin은 앞서 실행되는 CorsFilter가 검사합니다.
        if (!SAFE_METHODS.contains(request.getMethod())
            && !"OBSCURA".equals(request.getHeader("X-Requested-With"))) {
            response.setStatus(403);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("허용되지 않은 요청입니다.");
            return;
        }
        chain.doFilter(request, response);
    }
}
