package ru.yandex.notifications.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import ru.yandex.notifications.client.NotificationListener;
import ru.yandex.sharedlib.notification.NotificationDto;
import ru.yandex.notifications.config.KafkaConfig;
import ru.yandex.notifications.config.PostgresTestContainer;
import ru.yandex.notifications.service.NotificationService;
import ru.yandex.notifications.config.KafkaBrokerTestConfig;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "kafka-topics.notifications-topic=topic-bankapp-notifications",
                "spring.application.name=service-notifications-test",
                "spring.liquibase.enabled=false",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri="},
        classes = {NotificationListener.class, KafkaConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(topics = {"topic-bankapp-notifications"})
@MockBean(NotificationService.class)
public class NotificationServiceIntegrationTest extends PostgresTestContainer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    public void notificationSend_OkTest() {
        String testLogin = "user123";
        String testMessage = "Test notification message";
        NotificationDto notificationDto = new NotificationDto(testLogin, testMessage);

        Map<String, Object> consumerProps = KafkaBrokerTestConfig.createConsumerProps(embeddedKafkaBroker);

        try (var consumerForTest = new DefaultKafkaConsumerFactory<String, NotificationDto>(consumerProps).createConsumer()) {
            consumerForTest.subscribe(List.of("topic-bankapp-notifications"));

            kafkaTemplate.send("topic-bankapp-notifications", testLogin, notificationDto);

            var receivedMessage = KafkaTestUtils.getSingleRecord(consumerForTest, "topic-bankapp-notifications", Duration.ofSeconds(5));
            assertThat(receivedMessage.key()).isEqualTo(testLogin);
            assertThat(receivedMessage.value().getLogin()).isEqualTo(testLogin);
            assertThat(receivedMessage.value().getMessage()).isEqualTo(testMessage);
        }
    }
}