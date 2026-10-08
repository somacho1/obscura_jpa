package dev.jpa.obscura_jpa.auth;

import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@RequiredArgsConstructor
public class RoleInterceptor implements HandlerInterceptor {
    private final SessionAuthService auth;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method) {
            RequireRole rule = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequireRole.class);
            if (rule == null) {
                rule = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequireRole.class);
            }
            // 실제로 실행될 Controller 메서드에 붙은 권한을 검사합니다.
            if (rule != null) auth.requireRole(request, rule.value());
        }
        return true;
    }
}
