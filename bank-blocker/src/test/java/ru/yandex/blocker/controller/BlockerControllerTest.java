package ru.yandex.blocker.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.blocker.service.BlockerService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(BlockerController.class)
@WithMockUser
class BlockerControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @SpyBean
    private BlockerService blockerService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @BeforeEach
    void setup() {
        this.webTestClient = webTestClient.mutateWith(csrf());
    }

    @Test
    void postUserNotification_returnsTrue() {
        webTestClient.post()
                .uri(uri -> uri
                        .path("/api/block/{value}")
                        .build("200000")
                )
                .exchange()
                .expectStatus().isOk()
                .expectBody(Boolean.class).isEqualTo(true);
    }

    @Test
    void postUserNotification_returnsFalse() {
        webTestClient.post()
                .uri(uri -> uri
                        .path("/api/block/{value}")
                        .build(2000)
                )
                .exchange()
                .expectStatus().isOk()
                .expectBody(Boolean.class).isEqualTo(false);
    }

    @Test
    void postUserNotification_serviceError() {
        BigDecimal bdValue = BigDecimal.valueOf(2000);
        when(blockerService.isBlocked(bdValue)).thenReturn(Mono.error(new RuntimeException("failed")));

        webTestClient.post()
                .uri(uri -> uri
                        .path("/api/block/{value}")
                        .build(2000)
                )
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
