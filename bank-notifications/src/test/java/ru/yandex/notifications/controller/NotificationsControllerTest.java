package ru.yandex.notifications.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.sharedlib.notification.NotificationDto;
import ru.yandex.notifications.model.Notification;
import ru.yandex.notifications.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(NotificationsController.class)
@WithMockUser
class NotificationsControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private NotificationService notificationService;

    @BeforeEach
    void init() {
        this.webTestClient = webTestClient.mutateWith(csrf());
    }

    @Test
    void getUserNotifications_OkTest() {
        String login = "bob";
        LocalDateTime now = LocalDateTime.now();
        Notification n1 = Notification.builder().time(now.minusMinutes(2)).message("old").build();
        Notification n2 = Notification.builder().time(now).message("new").build();
        Notification n3 = Notification.builder().time(now.minusMinutes(1)).message("not so old").build();

        when(notificationService.getUserNotifications(login))
                .thenReturn(Flux.just(n1, n2, n3));

        webTestClient.get()
                .uri("/api/{login}/notifications", login)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NotificationDto.class)
                .hasSize(3)
                .value(list -> {
                    assertEquals(List.of("new", "not so old", "old"),
                            list.stream().map(NotificationDto::getMessage).toList());
                });
    }

    @Test
    void getUserNotifications_5xxErrorTest() {
        when(notificationService.getUserNotifications("alice"))
                .thenReturn(Flux.error(new RuntimeException("boom")));

        webTestClient.get()
                .uri("/api/{login}/notifications", "alice")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void sendUserNotification_OkTest() {
        String login = "bob";
        String msg = "Hello!";
        Notification created = new Notification(); created.setMessage(msg);
        when(notificationService.notify(login, msg))
                .thenReturn(Mono.just(created));

        webTestClient.post()
                .uri("/api/{login}/notifications", login)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(msg)
                .exchange()
                .expectStatus().isOk()
                .expectBody(NotificationDto.class)
                .value(dto -> assertEquals(msg, dto.getMessage()));
    }

    @Test
    void sendUserNotification_5xxErrorTest() {
        String login = "bob";
        when(notificationService.notify(eq(login), anyString()))
                .thenReturn(Mono.error(new RuntimeException("fail")));

        webTestClient.post()
                .uri("/api/{login}/notifications", login)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("msg")
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
