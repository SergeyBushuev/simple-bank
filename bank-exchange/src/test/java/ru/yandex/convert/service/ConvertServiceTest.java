package ru.yandex.convert.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import ru.yandex.convert.client.ExchangeClient;
import ru.yandex.sharedlib.account.CurrencyDto;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConvertServiceTest {

    @Mock
    private ExchangeClient exchangeClient;

    @InjectMocks
    private ConvertService convertService;

    @Test
    void convert_OkTest() {
        List<CurrencyDto> currencies = List.of(
                new CurrencyDto("USD", "USD", new BigDecimal("1.2")),
                new CurrencyDto("EUR", "EUR", new BigDecimal("0.8"))
        );
        when(exchangeClient.getCurrencies()).thenReturn(Flux.fromIterable(currencies));
        BigDecimal result = convertService
                .convertAmount("USD", "EUR", new BigDecimal("100"))
                .block();
        assertEquals(new BigDecimal("150.0"), result);
    }
}
