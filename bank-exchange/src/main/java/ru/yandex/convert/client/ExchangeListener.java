package ru.yandex.convert.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.yandex.sharedlib.account.CurrencyDto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeListener {

    private final Map<String, BigDecimal> currencyRates = new ConcurrentHashMap<>();

    @KafkaListener(
            topics = "${kafka-topics.exchange-rates-topic}",
            groupId = "${spring.application.name}.exchange-rate-consumer-group")
    public void updateCurrencyRate(@Header(KafkaHeaders.RECEIVED_KEY) String currency,
                                   @Payload CurrencyDto message) {
        log.info("Currency exchange rate received: {} = {}", currency, message);
        currencyRates.put(currency, message.getValue());
    }

    public Optional<BigDecimal> getRate(String currency) {
        return Optional.ofNullable(currencyRates.get(currency));
    }
}