package ru.yandex.transfer.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import ru.yandex.sharedlib.account.AccountDto;
import ru.yandex.sharedlib.account.Currency;
import ru.yandex.sharedlib.cash.CashAction;
import ru.yandex.sharedlib.cash.CashProcessResponse;
import ru.yandex.sharedlib.cash.CashRequest;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountsClientTest {

    private WireMockServer wireMock;
    private AccountsClient accountsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() throws Exception {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        WebClient webClient = WebClient.builder().build();
        accountsClient = new AccountsClient(webClient);
        ReflectionTestUtils.setField(accountsClient,
                "gateway", "localhost:" + wireMock.port());


        wireMock.stubFor(post(urlEqualTo("/api/john/cash"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(getCashRequest())))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(getCashProcessResponse())))
        );

        wireMock.stubFor(get(urlEqualTo("/api/john/account/USD"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(getAccount())))
        );
    }

    @AfterEach
    void clear() {
        wireMock.stop();
    }

    @Test
    void processCash_okTest() {
        CashRequest req = CashRequest.builder()
                .currency("USD")
                .action(CashAction.PUT)
                .value("150.00")
                .build();

        CashProcessResponse resp = accountsClient.processCash("john", req).block();
        assertNotNull(resp);
        assertEquals("completed", resp.getStatus());
        assertNull(resp.getErrors());
    }

    @Test
    void getAccount_okTest() {
        AccountDto dto = accountsClient.getAccount("john", "USD").block();
        assertNotNull(dto);
        assertEquals(Currency.USD, dto.getCurrency());
        assertEquals(0, dto.getValue().compareTo(new BigDecimal("200.00")));
        assertTrue(dto.isExists());
    }

    private CashRequest getCashRequest() {
        return CashRequest.builder()
                .currency("USD")
                .action(CashAction.PUT)
                .value("150.00")
                .build();
    }

    private CashProcessResponse getCashProcessResponse() {
        return CashProcessResponse.builder()
                .status("completed")
                .errors(null)
                .build();
    }

    private AccountDto getAccount() {
        return new AccountDto(1L, Currency.USD,
                new BigDecimal("200.00"),
                true);
    }
}
