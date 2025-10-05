package ru.yandex.exchange.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ExchangeRateService exchangeRateService;

    @Value("${kafka-topics.exchange-rates-topic}")
    private String exchangeRateTopic;

    @Scheduled(fixedRate = 1000)
    public void publishCurrentRates() {
        exchangeRateService.getAllCurrentRates()
                .subscribe(rate -> {
                    ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(exchangeRateTopic, rate.getTitle(), rate);
                    kafkaTemplate.send(producerRecord);
                    log.info("Отправлено сообщение {} в топик {}", producerRecord.value(), exchangeRateTopic);
                });
    }


}