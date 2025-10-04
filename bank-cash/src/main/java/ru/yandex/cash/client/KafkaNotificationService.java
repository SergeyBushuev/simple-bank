package ru.yandex.cash.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.sharedlib.notification.NotificationDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaNotificationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka-topics.notifications-topic}")
    private String notificationsTopic;

    public void sendNotification(String key, NotificationDto message) {
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(notificationsTopic, key, message);
        kafkaTemplate.send(producerRecord);
        log.info("Отправлено сообщение {} в топик {}", message, notificationsTopic);
    }
}