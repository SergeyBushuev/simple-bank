package ru.yandex.transfer.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.yandex.sharedlib.transfer.TransferRequest;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConvertClientTest {

    private WireMockServer wireMock;
    private ConvertClient convertClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    @SneakyThrows
    void setup() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        WebClient webClient = WebClient.builder().build();
        convertClient = new ConvertClient(webClient);
        ReflectionTestUtils.setField(
                convertClient,
                "gateway",
                "localhost:" + wireMock.port()
        );
    }

    @AfterEach
    void clear() {
        wireMock.stop();
    }

    @Test
    @SneakyThrows
    void convertAmount_OkTest() {
        TransferRequest request = TransferRequest.builder()
                .fromCurrency("USD")
                .toCurrency("EUR")
                .value(new BigDecimal("150.00"))
                .build();
        String requestJson = objectMapper.writeValueAsString(request);

        BigDecimal expected = new BigDecimal("123.45");
        wireMock.stubFor(post(urlEqualTo("/api/convert"))
                .withRequestBody(equalToJson(requestJson))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(expected.toString())
                )
        );

        BigDecimal result = convertClient.convertAmount(request).block();
        assertNotNull(result);
        assertEquals(0, result.compareTo(expected));
    }

    @Test
    @SneakyThrows
    void convertAmount_ErrorTest() {
        wireMock.stubFor(post(urlEqualTo("/api/convert"))
                .willReturn(aResponse().withStatus(500))
        );

        TransferRequest request = TransferRequest.builder()
                .fromCurrency("USD")
                .toCurrency("EUR")
                .value(new BigDecimal("100.00"))
                .build();

        assertThrows(Exception.class, () -> convertClient.convertAmount(request).block());
    }
}
