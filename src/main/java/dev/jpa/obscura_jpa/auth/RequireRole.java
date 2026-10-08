package dev.jpa.obscura_jpa.auth;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    // ADMIN에는 SUPER_ADMIN도 포함됩니다.
    String value() default "ADMIN";
}
