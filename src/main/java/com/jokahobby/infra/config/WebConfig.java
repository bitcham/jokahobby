package com.jokahobby.infra.config;

import com.jokahobby.infra.logging.RequestTracingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<RequestTracingFilter> requestTracingFilter() {
        FilterRegistrationBean<RequestTracingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestTracingFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
