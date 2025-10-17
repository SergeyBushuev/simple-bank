package ru.yandex.convert.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.convert.client.ExchangeListener;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvertService {

    private final ExchangeListener exchangeClient;
    private final MeterRegistry metrics;

    public Mono<BigDecimal> convertAmount(String fromCurrency,
                                          String toCurrency,
                                          BigDecimal value) {

        return Mono.fromSupplier(() -> {
            BigDecimal fromRate = getCurrencyRate(fromCurrency);
            BigDecimal toRate = getCurrencyRate(toCurrency);
            Counter.builder("exchange")
                    .tags("curr_from", fromCurrency)
                    .tags("curr_to", toCurrency)
                    .register(metrics)
                    .increment();
            return value.multiply(fromRate).divide(toRate, RoundingMode.HALF_UP);
        });
    }
    private BigDecimal getCurrencyRate(String currency) {
        return exchangeClient.getRate(currency)
                .orElseThrow(() -> new IllegalArgumentException("Can't find rate for " + currency));
    }
}
