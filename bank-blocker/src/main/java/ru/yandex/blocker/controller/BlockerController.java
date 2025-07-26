package ru.yandex.blocker.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.yandex.blocker.service.BlockerService;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class BlockerController {

    private final BlockerService blockerService;

    @PostMapping("/api/block/{value}")
    public Mono<Boolean> postUserNotification(@PathVariable String value) {
        long lValue = Long.parseLong(value);
        BigDecimal bdValue = BigDecimal.valueOf(lValue);
        return blockerService.isBlocked(bdValue);
    }

}
