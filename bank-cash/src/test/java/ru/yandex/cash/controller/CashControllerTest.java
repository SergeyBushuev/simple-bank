package ru.yandex.cash.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import ru.yandex.cash.service.CashService;
import ru.yandex.sharedlib.cash.CashRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(CashController.class)
@WithMockUser
class CashControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CashService cashService;

    @BeforeEach
    void init() {
        this.webTestClient = webTestClient.mutateWith(csrf());
    }

    @Test
    void updateCashBalance_OkTest() {
        when(cashService.updateCashBalance(eq("bob"), any(CashRequest.class)))
                .thenReturn(Mono.just(ResponseEntity.ok().<Void>build()));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("currency", "USD");
        body.add("action", "PUT");
        body.add("value", "100.00");

        webTestClient.post()
                .uri("/user/bob/cash")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
    }

    @Test
    void updateCashBalance_ExceptionTest() {
        when(cashService.updateCashBalance(eq("bob"), any(CashRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("failed")));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("currency", "EUR");
        body.add("action", "TAKE");
        body.add("value", "50.00");

        webTestClient.post()
                .uri("/user/bob/cash")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
