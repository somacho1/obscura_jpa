package dev.jpa.obscura_jpa.tool;

import java.io.File;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 리소스 설정
 *
 * 서버에 저장된 상품 이미지를 브라우저에서 접근할 수 있도록
 * /uploads/** URL과 실제 uploads 폴더를 연결한다.
 *
 * 예)
 * 실제 파일:
 * uploads/products/main/abc.jpg
 *
 * 브라우저:
 * http://localhost:9101/uploads/products/main/abc.jpg
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 프로젝트 실행 위치의 uploads 폴더 절대경로
        String uploadPath = new File("uploads").getAbsoluteFile().toURI().toString();

        // /uploads/** 요청을 실제 uploads 폴더와 연결
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(uploadPath);
    }
}