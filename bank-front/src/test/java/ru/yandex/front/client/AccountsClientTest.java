package ru.yandex.front.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.yandex.sharedlib.account.AccountDto;
import ru.yandex.sharedlib.account.Currency;
import ru.yandex.sharedlib.account.UserDto;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountsClientTest {

    private WireMockServer wireMock;
    private AccountsClient accountsClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();

        WebClient webClient = WebClient.builder().build();
        accountsClient = new AccountsClient(webClient);
        ReflectionTestUtils.setField(
                accountsClient,
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
    void getUser_OkTest() {
        UserDto stubUser = UserDto.builder()
                .login("alice")
                .name("Alice A")
                .password("secret")
                .role("USER")
                .build();
        String json = mapper.writeValueAsString(stubUser);

        wireMock.stubFor(get(urlPathEqualTo("/api/alice/user"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)
                ));

        Mono<UserDto> mono = accountsClient.getUserDto("alice");
        UserDto result = mono.block();

        assertNotNull(result);
        assertEquals(stubUser.getLogin(), result.getLogin());
        assertEquals(stubUser.getName(), result.getName());
        assertEquals(stubUser.getRole(), result.getRole());
    }

    @Test
    @SneakyThrows
    void getAccounts_OkTest() {
        List<AccountDto> stubAccounts = List.of(
                new AccountDto(1L, Currency.RUB, new BigDecimal("50.00"), true),
                new AccountDto(2L, Currency.USD, new BigDecimal("100.00"), true)
        );
        String json = mapper.writeValueAsString(stubAccounts);

        wireMock.stubFor(get(urlPathEqualTo("/api/bob/accounts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)
                ));

        Mono<List<AccountDto>> mono = accountsClient.getAccountsList("bob");
        List<AccountDto> result = mono.block();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(Currency.RUB, result.get(0).getCurrency());
        assertEquals(Currency.USD, result.get(1).getCurrency());
    }

    @Test
    @SneakyThrows
    void getUsers_OkTest() {
        List<UserDto> stubUsers = List.of(
                UserDto.builder().login("u1").name("User1").build(),
                UserDto.builder().login("u2").name("User2").build()
        );
        String json = mapper.writeValueAsString(stubUsers);

        wireMock.stubFor(get(urlPathEqualTo("/api/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)
                ));

        Mono<List<UserDto>> mono = accountsClient.getUsers();
        List<UserDto> result = mono.block();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getLogin().equals("u1")));
        assertTrue(result.stream().anyMatch(u -> u.getLogin().equals("u2")));
    }

    @Test
    @SneakyThrows
    void getCurrencies_OkTest() {
        List<Currency> stubCurrencies = List.of(
                Currency.RUB,
                Currency.USD
        );
        String json = mapper.writeValueAsString(stubCurrencies);

        wireMock.stubFor(get(urlPathEqualTo("/api/currencies"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)
                ));

        Mono<List<Currency>> mono = accountsClient.getCurrencies();
        List<Currency> result = mono.block();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(Currency.RUB.getTitle(), result.get(0).getTitle());
        assertEquals(Currency.USD.getTitle(), result.get(1).getTitle());
    }
}
