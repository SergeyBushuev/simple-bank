package ru.yandex.cash.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class BlockersClient {

    private final WebClient webClient;

    @Value("${services.gateway-server.name}")
    private String gateway;

    @Retry(name = "gateway-service")
    @CircuitBreaker(name = "gateway-service", fallbackMethod = "sendBlockerRequestFallback")
    public Mono<Boolean> sendBlockerRequest(BigDecimal value) {
        return webClient.post()
                .uri("http://" + gateway + "/api/block/{value}", value)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptCharset(StandardCharsets.UTF_8)
                .retrieve()
                .bodyToMono(Boolean.class);
    }

    private Mono<Boolean> sendBlockerRequestFallback() {
        return Mono.just(Boolean.TRUE);
    }
}
