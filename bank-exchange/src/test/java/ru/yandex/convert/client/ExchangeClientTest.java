package ru.yandex.convert.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import ru.yandex.convert.config.KafkaClientConfig;
import ru.yandex.convert.config.KafkaBrokerTestConfig;
import ru.yandex.convert.config.PostgresTestContainer;
import ru.yandex.sharedlib.account.CurrencyDto;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "kafka-topics.exchange-rates-topic=topic-bankapp-exchange-rates",
                "spring.application.name=service-convert-test",
                "spring.liquibase.enabled=false",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri="},
        classes = {ExchangeListener.class, KafkaClientConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(topics = {"topic-bankapp-exchange-rates"})
public class ExchangeClientTest extends PostgresTestContainer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    public void testExchangeRateListenerProcessesMessage() {
        String testCurrency = "USD";
        BigDecimal testRate = new BigDecimal("74.50");
        CurrencyDto currencyDto = CurrencyDto.builder()
                .title(testCurrency)
                .name("US Dollar")
                .value(testRate)
                .build();

        Map<String, Object> consumerProps = KafkaBrokerTestConfig.createConsumerProps(embeddedKafkaBroker);

        try (var consumerForTest = new DefaultKafkaConsumerFactory<String, CurrencyDto>(consumerProps).createConsumer()) {
            consumerForTest.subscribe(List.of("topic-bankapp-exchange-rates"));

            kafkaTemplate.send("topic-bankapp-exchange-rates", testCurrency, currencyDto);

            var receivedMessage = KafkaTestUtils.getSingleRecord(consumerForTest, "topic-bankapp-exchange-rates", Duration.ofSeconds(5));
            assertThat(receivedMessage.key()).isEqualTo(testCurrency);
            assertThat(receivedMessage.value().getTitle()).isEqualTo(testCurrency);
            assertThat(receivedMessage.value().getValue()).isEqualTo(testRate);
        }
    }
}