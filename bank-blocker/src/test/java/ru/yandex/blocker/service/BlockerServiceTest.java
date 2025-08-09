package ru.yandex.blocker.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BlockerServiceTest {

    private final BlockerService blockerService = new BlockerService();

    @Test
    void shouldBlockAt21Hours() {
        BigDecimal value = BigDecimal.valueOf(200000);
        Boolean result = blockerService.isBlocked(value).block();

        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void shouldNotBlockBefore21Hours() {
        BigDecimal value = BigDecimal.valueOf(2000);
        Boolean result = blockerService.isBlocked(value).block();
        assertEquals(Boolean.FALSE, result);
    }


}
