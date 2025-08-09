package ru.yandex.exchange.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import ru.yandex.exchange.service.ExchangeRateService;
import ru.yandex.sharedlib.account.CurrencyDto;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@WebFluxTest(ExchangeRatesController.class)
@WithMockUser
class ExchangeRatesControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @SpyBean
    private ExchangeRateService exchangeRateService;

    @Test
    void getCurrencies_OkTest() {
        CurrencyDto c1 = new CurrencyDto("USD", "US Dollar", new BigDecimal("91.4"));
        CurrencyDto c2 = new CurrencyDto("BYN", "Belorussian ruble", new BigDecimal("5.7"));

        webTestClient.get()
                .uri("/api/rates")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CurrencyDto.class)
                .hasSize(3)
                .value(list -> {
                    CurrencyDto dto1 = list.get(0);
                    CurrencyDto dto2 = list.get(1);
                    CurrencyDto dto3 = list.get(2);
                    assertEquals("Belorussian ruble", dto1.getName());
                    assertEquals("BYN", dto1.getTitle());
                    assertEquals(-1, dto1.getValue().compareTo(new BigDecimal("26")));
                    assertEquals("US Dollar", dto2.getName());
                    assertEquals("USD", dto2.getTitle());
                    assertEquals(-1, dto2.getValue().compareTo(new BigDecimal("96")));
                    assertEquals("Ruble", dto3.getName());
                    assertEquals("RUB", dto3.getTitle());
                    assertEquals(0, dto3.getValue().compareTo(new BigDecimal("1")));
                });
    }

    @Test
    void getCurrencies_serviceError() {
        when(exchangeRateService.getAllCurrentRates())
                .thenReturn(Flux.error(new RuntimeException("Service failure")));

        webTestClient.get()
                .uri("/api/rates")
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
