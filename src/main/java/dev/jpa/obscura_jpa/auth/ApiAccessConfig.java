package dev.jpa.obscura_jpa.auth;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.*;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@RequiredArgsConstructor
public class ApiAccessConfig implements WebMvcConfigurer {
    private final SessionAuthService auth;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RoleInterceptor(auth)).addPathPatterns("/api/**");
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> apiCorsFilter(
        @Value("${obscura.cors.allowed-origins:http://localhost:5173,http://localhost:5174}") String origins
    ) {
        List<String> allowed = Arrays.stream(origins.split(",")).map(String::trim)
            .filter(value -> !value.isEmpty()).toList();
        if (allowed.isEmpty() || allowed.contains("*")) {
            throw new IllegalArgumentException("CORS에는 실제 React 주소를 지정해야 합니다.");
        }
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowed);
        config.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(-200);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ApiRequestFilter> apiRequestFilter() {
        FilterRegistrationBean<ApiRequestFilter> registration =
            new FilterRegistrationBean<>(new ApiRequestFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(-100);
        return registration;
    }
}
