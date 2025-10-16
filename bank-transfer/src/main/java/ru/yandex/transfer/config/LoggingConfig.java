package ru.yandex.transfer.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@Slf4j
public class LoggingConfig {

    @PostConstruct
    public void init() {
        log.info("Bank Accounts service logging initialized with Log4j2");
    }

    @Bean
    public OncePerRequestFilter loggingFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                long startTime = System.currentTimeMillis();
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    if (log.isInfoEnabled()) {
                        log.info("Account API: method={} uri={} status={} duration={}ms",
                                request.getMethod(),
                                request.getRequestURI(),
                                response.getStatus(),
                                duration);
                    }
                }
            }
        };
    }
}