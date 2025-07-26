package ru.yandex.blocker.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class BlockerService {

    public Mono<Boolean> isBlocked(BigDecimal value) {
        long lValue = value.longValue();
        return Mono.just(lValue >= 200000L);
    }
}
