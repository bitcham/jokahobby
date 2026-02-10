package com.jokahobby.infra.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.modelmapper.AbstractConverter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.NameTokenizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Configuration
@RequiredArgsConstructor
@EnableScheduling
public class AppConfig {

    private final EntityManager entityManager;

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setDestinationNameTokenizer(NameTokenizers.UNDERSCORE)
                .setSourceNameTokenizer(NameTokenizers.UNDERSCORE);
        modelMapper.addConverter(new AbstractConverter<LocalDateTime, Instant>() {
            @Override
            protected Instant convert(LocalDateTime source) {
                return source == null ? null : source.toInstant(ZoneOffset.UTC);
            }
        });
        return modelMapper;
    }

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
