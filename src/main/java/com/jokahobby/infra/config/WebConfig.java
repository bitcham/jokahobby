package com.jokahobby.infra.config;

import com.jokahobby.modules.notification.NotificationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final NotificationInterceptor notificationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludePatterns = List.of(
                "/css/**", "/js/**", "/images/**", "/webjars/**",
                "/favicon.ico", "/node_modules/**", "/api/**", "/oauth2/**"
        );

        registry.addInterceptor(notificationInterceptor)
                .excludePathPatterns(excludePatterns);
    }
}
