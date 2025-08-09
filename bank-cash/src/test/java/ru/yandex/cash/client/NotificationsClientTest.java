package ru.yandex.cash.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.yandex.sharedlib.notification.NotificationDto;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationsClientTest {

    private WireMockServer wireMock;
    private NotificationsClient notificationsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() throws Exception {
        wireMock = new WireMockServer(
                WireMockConfiguration.options()
                        .dynamicPort());
        wireMock.start();

        WebClient webClient = WebClient.builder().build();
        notificationsClient = new NotificationsClient(webClient);
        ReflectionTestUtils.setField(
                notificationsClient,
                "gateway",
                "localhost:" + wireMock.port()
        );

        wireMock.stubFor(post(urlPathEqualTo("/api/bob/notifications"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalTo("Success"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBody(objectMapper.writeValueAsString(getNotification()))
                ));
    }

    @AfterEach
    void clear() {
        wireMock.stop();
    }

    @Test
    void sendNotification_OkTest() {
        Mono<NotificationDto> mono = notificationsClient.sendNotification("bob", "Success");
        NotificationDto result = mono.block();

        assertNotNull(result);
        assertEquals("Success", result.getMessage());
    }

    @Test
    void sendNotification_ExceptionTest() {
        wireMock.stubFor(post(urlPathEqualTo("/api/alice/notifications"))
                .willReturn(aResponse().withStatus(500)));

        assertThrows(Exception.class, () -> notificationsClient.sendNotification("alice", "Test").block());
    }

    private NotificationDto getNotification() {
        return NotificationDto.builder()
                .message("Success")
                .build();
    }
}
