package ru.yandex.transfer.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockersClientTest {

    private WireMockServer wireMockServer;
    private BlockersClient blockersClient;

    @BeforeEach
    void setup() {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.options().dynamicPort()
        );
        wireMockServer.start();

        WebClient webClient = WebClient.builder().build();
        blockersClient = new BlockersClient(webClient);
        ReflectionTestUtils.setField(
                blockersClient,
                "gateway",
                "localhost:" + wireMockServer.port()
        );
    }

    @AfterEach
    void clear() {
        wireMockServer.stop();
    }

    @Test
    void sendBlockerRequestBlocked_OkTest() {
        wireMockServer.stubFor(post(urlPathMatching("/api/block/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")
                )
        );

        Boolean result = blockersClient
                .sendBlockerRequest(BigDecimal.valueOf(200000L))
                .block();

        assertNotNull(result);
        assertTrue(result);
    }

    @Test
    void sendBlockerRequestNotBlocked_OkTest() {
        wireMockServer.stubFor(post(urlPathMatching("/api/block/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("false")
                )
        );

        Boolean result = blockersClient
                .sendBlockerRequest(BigDecimal.valueOf(20000L))
                .block();

        assertNotNull(result);
        assertFalse(result);
    }

    @Test
    void sendBlockerRequest_ErrorTest() {
        wireMockServer.stubFor(post(urlPathMatching("/api/block/.*"))
                .willReturn(aResponse()
                        .withStatus(500)
                )
        );

        assertThrows(Exception.class, () -> blockersClient.sendBlockerRequest(BigDecimal.valueOf(20000L)).block());
    }
}
