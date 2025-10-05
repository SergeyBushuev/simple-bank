package ru.yandex.notifications.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.sharedlib.notification.NotificationDto;
import ru.yandex.notifications.model.Notification;
import ru.yandex.notifications.service.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService service;

    @KafkaListener(
            topics = "${kafka-topics.notifications-topic}",
            groupId = "${spring.application.name}.notifications-consumer-group")
    public Mono<Notification> listenNotifications(@Payload NotificationDto notification) {
        log.info("Message received {}", notification);
        return service.notify(notification.getLogin(), notification.getMessage());
    }
}