package com.dreamarchive.config;

import com.dreamarchive.interceptor.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    @Value("${file.upload-path:uploads/}")
    private String uploadPath;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/uploads/**",
                        "/error"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(toFileLocation(uploadPath));
    }

    private String toFileLocation(String path) {
        if (path == null || path.isBlank()) {
            return "file:uploads/";
        }
        String p = path.replace("\\", "/");
        if (!p.endsWith("/")) {
            p = p + "/";
        }
        if (p.startsWith("file:")) {
            return p;
        }
        return "file:" + p;
    }
}
