package ru.yandex.convert.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.convert.client.ExchangeListener;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConvertServiceTest {

    @Mock
    private ExchangeListener exchangeClient;

    @InjectMocks
    private ConvertService convertService;

    @Test
    void convert_OkTest() {
        when(exchangeClient.getRate("USD")).thenReturn(Optional.of(new BigDecimal("1.2")));
        when(exchangeClient.getRate("EUR")).thenReturn(Optional.of(new BigDecimal("0.8")));

        BigDecimal result = convertService
                .convertAmount("USD", "EUR", new BigDecimal("100"))
                .block();
        assertEquals(0, new BigDecimal("150.0").compareTo(result));
    }
}
