package ru.yandex.exchange.controller;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Flux;
import ru.yandex.exchange.service.ExchangeRateProducer;
import ru.yandex.sharedlib.account.CurrencyDto;
import ru.yandex.exchange.service.ExchangeRateService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ExchangeRateService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "kafka-topics.exchange-rates-topic=topic-bankapp-exchange-rates")
class ExchangeProducerServiceTest {

    @Autowired
    private ExchangeRateProducer exchangeProducerService;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Test
    void ratesToKafka_OkTest() {
        CurrencyDto testCurrency = CurrencyDto.builder()
                .title("USD")
                .name("US Dollar")
                .value(new BigDecimal("74.50"))
                .build();

        when(exchangeRateService.getAllCurrentRates())
                .thenReturn(Flux.just(testCurrency));

        exchangeProducerService.publishCurrentRates();

        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }

    @Test
    void multipleRatesToKafka_OkTest() {
        CurrencyDto usd = CurrencyDto.builder()
                .title("USD")
                .name("US Dollar")
                .value(new BigDecimal("74.50"))
                .build();

        CurrencyDto eur = CurrencyDto.builder()
                .title("EUR")
                .name("Euro")
                .value(new BigDecimal("82.30"))
                .build();

        when(exchangeRateService.getAllCurrentRates())
                .thenReturn(Flux.just(usd, eur));

        exchangeProducerService.publishCurrentRates();

        verify(kafkaTemplate, times(2)).send(any(ProducerRecord.class));
    }

    @Test
    void ratesSamePartitionToKafka_OkTest() {
        CurrencyDto usd1 = CurrencyDto.builder()
                .title("USD")
                .name("US Dollar")
                .value(new BigDecimal("74.50"))
                .build();

        CurrencyDto usd2 = CurrencyDto.builder()
                .title("USD")
                .name("US Dollar")
                .value(new BigDecimal("74.60"))
                .build();

        when(exchangeRateService.getAllCurrentRates())
                .thenReturn(Flux.just(usd1, usd2));
        exchangeProducerService.publishCurrentRates();
        verify(kafkaTemplate, times(2)).send(any(ProducerRecord.class));
    }
}