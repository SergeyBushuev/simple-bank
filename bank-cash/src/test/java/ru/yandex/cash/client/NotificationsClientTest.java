package ru.yandex.cash.client;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import ru.yandex.sharedlib.notification.NotificationDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@SpringBootTest(classes = KafkaNotificationService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "kafka-topics.notifications-topic=topic-bankapp-notifications")
class NotificationsProducerServiceTest {

    @Autowired
    private KafkaNotificationService notificationsProducerService;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void sendMessage_OkTest() {
        String testKey = "user123";
        NotificationDto testNotification = new NotificationDto("user123", "Test notification message");

        notificationsProducerService.sendNotification(testKey, testNotification);

        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }

    @Test
    void sendSeveralMessages_OkTest() {
        NotificationDto notification1 = new NotificationDto("user1", "Message 1");
        NotificationDto notification2 = new NotificationDto("user2", "Message 2");

        notificationsProducerService.sendNotification("key1", notification1);
        notificationsProducerService.sendNotification("key2", notification2);

        verify(kafkaTemplate, times(2)).send(any(ProducerRecord.class));
    }

    @Test
    void sendMessageToSamePartition_OkTest() {
        String sameKey = "user123";
        NotificationDto notification1 = new NotificationDto("user123", "First message");
        NotificationDto notification2 = new NotificationDto("user123", "Second message");

        notificationsProducerService.sendNotification(sameKey, notification1);
        notificationsProducerService.sendNotification(sameKey, notification2);

        verify(kafkaTemplate, times(2)).send(any(ProducerRecord.class));
    }
}