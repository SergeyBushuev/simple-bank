package ru.yandex.accounts.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.accounts.model.Account;
import ru.yandex.accounts.model.User;
import ru.yandex.accounts.service.AccountService;
import ru.yandex.accounts.service.UserService;
import ru.yandex.accounts.validation.ValidationUtils;
import ru.yandex.sharedlib.account.AccountDto;
import ru.yandex.sharedlib.account.Currency;
import ru.yandex.sharedlib.account.UserDto;
import ru.yandex.sharedlib.cash.CashAction;
import ru.yandex.sharedlib.cash.CashProcessResponse;
import ru.yandex.sharedlib.cash.CashRequest;
import ru.yandex.sharedlib.settings.EditAccountsRequest;
import ru.yandex.sharedlib.signup.SignupRequest;
import ru.yandex.sharedlib.signup.SignupResponse;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(controllers = AccountsController.class)
@WithMockUser
class AccountsControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserService userService;

    @MockBean
    private AccountService accountService;

    @BeforeEach
    void setup() {
        this.webTestClient = webTestClient.mutateWith(csrf());
    }

    @Test
    void getUser_OkTest() {
        UserDto userDto = UserDto.builder()
                .login("testuser")
                .name("Test User")
                .build();

        when(userService.findByUsername("testuser"))
                .thenReturn(Mono.just(userDto));

        webTestClient.get()
                .uri("/api/testuser/user")
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto.class)
                .isEqualTo(userDto);
    }

    @Test
    void getUserAccount_OkTest() {
        Account usdAccount = createAccount("USD", new BigDecimal("100.00"));
        Account eurAccount = createAccount("EUR", new BigDecimal("50.00"));

        when(accountService.getUserAccounts("testuser"))
                .thenReturn(Flux.just(usdAccount, eurAccount));

        webTestClient.get()
                .uri("/api/testuser/accounts")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Account.class)
                .hasSize(3);
    }

    @Test
    void getAllUsers_OkTest() {
        User user1 = createUser("user1", "User One");
        User user2 = createUser("user2", "User Two");

        when(userService.findAllUsers())
                .thenReturn(Flux.just(user1, user2));

        webTestClient.get()
                .uri("/api/users")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].login").isEqualTo("user1")
                .jsonPath("$[0].name").isEqualTo("User One")
                .jsonPath("$[1].login").isEqualTo("user2")
                .jsonPath("$[1].name").isEqualTo("User Two");
    }

    @Test
    void getCurrencies_OkTest() {
        webTestClient.get()
                .uri("/api/currencies")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Currency.class)
                .hasSize(3);
    }

    @Test
    void registerUser_OkTest() {
        SignupRequest signupRequest = SignupRequest.builder()
                .login("newuser")
                .name("New User")
                .password("password123")
                .confirmPassword("password123")
                .birthdate("1990-01-01")
                .build();

        User newUser = createUser("newuser", "New User");

        try (MockedStatic<ValidationUtils> utilities = mockStatic(ValidationUtils.class)) {
            utilities.when(() -> ValidationUtils.validateSignupRequest(any()))
                    .thenReturn(List.of());

            when(userService.registerNewUser(any(SignupRequest.class)))
                    .thenReturn(Mono.just(newUser));

            webTestClient.post()
                    .uri("/api/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(signupRequest)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SignupResponse.class)
                    .value(response -> {
                        assertEquals("newuser", response.getLogin());
                        assertEquals("New User", response.getName());
                        assertNull(response.getErrors());
                    });
        }
    }

    @Test
    void signup_ExceptionTest() {
        SignupRequest signupRequest = SignupRequest.builder()
                .login("user")
                .name("User")
                .password("pass")
                .birthdate("2020-01-01")
                .build();

        List<String> errors = Arrays.asList(
                "Повторный пароль не должен быть пустым",
                "Пароли должны совпадать", "Возраст должен быть старше 18 лет");

        try (MockedStatic<ValidationUtils> utilities = mockStatic(ValidationUtils.class)) {
            utilities.when(() -> ValidationUtils.validateSignupRequest(any()))
                    .thenReturn(errors);

            webTestClient.post()
                    .uri("/api/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(signupRequest)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SignupResponse.class)
                    .value(response -> {
                        assertEquals(errors, response.getErrors());
                    });
        }
    }

    @Test
    void editUser_OkTest() {
        EditAccountsRequest request = new EditAccountsRequest();
        request.setName("Updated Name");
        request.setAccount(Arrays.asList("USD", "EUR"));

        try (MockedStatic<ValidationUtils> utilities = mockStatic(ValidationUtils.class)) {
            utilities.when(() -> ValidationUtils.validateEditUserAccountsRequest(any()))
                    .thenReturn(List.of());

            when(userService.updateUserInfo(eq("testuser"), any(EditAccountsRequest.class)))
                    .thenReturn(Mono.empty());
            when(accountService.updateAccounts(eq("testuser"), anyList()))
                    .thenReturn(Mono.empty());

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("name", "Updated Name");
            formData.addAll("account", Arrays.asList("USD", "EUR"));

            webTestClient.post()
                    .uri("/user/testuser/editUserAccounts")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formData)
                    .exchange()
                    .expectStatus().is3xxRedirection()
                    .expectHeader().location("/?login=testuser");
        }
    }

    @Test
    void editCash_OkTest() {
        CashRequest cashRequest = new CashRequest();
        cashRequest.setCurrency("USD");
        cashRequest.setValue("50.00");
        cashRequest.setAction(CashAction.PUT);

        Account account = createAccount("USD", new BigDecimal("100.00"));

        try (MockedStatic<ValidationUtils> utilities = mockStatic(ValidationUtils.class)) {
            utilities.when(() -> ValidationUtils.validateEditCashRequest(any(), any()))
                    .thenReturn(List.of());

            when(accountService.getAccount("testuser", "USD"))
                    .thenReturn(Mono.just(account));
            when(accountService.updateCashBalance(eq("testuser"), any(CashRequest.class)))
                    .thenReturn(Mono.empty());

            webTestClient.post()
                    .uri("/api/testuser/cash")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(cashRequest)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(CashProcessResponse.class)
                    .value(response -> {
                        assertEquals("completed", response.getStatus());
                    });
        }
    }

    @Test
    void cash_AccountNotFoundTest() {
        CashRequest cashRequest = new CashRequest();
        cashRequest.setCurrency("USD");
        cashRequest.setValue("50.00");
        cashRequest.setAction(CashAction.PUT);

        when(accountService.getAccount("testuser", "USD"))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/api/testuser/cash")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cashRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CashProcessResponse.class)
                .value(response -> {
                    assertEquals("error", response.getStatus());
                    assertTrue(response.getErrors().contains("Account not found USD"));
                });
    }

    @Test
    void cash_invalidRequestTest() {
        CashRequest cashRequest = new CashRequest();
        cashRequest.setCurrency("USD");
        cashRequest.setValue("1000.00");
        cashRequest.setAction(CashAction.GET);

        Account account = createAccount("USD", new BigDecimal("100.00"));
        List<String> errors = List.of("Not enough funds USD 1000.00");

        try (MockedStatic<ValidationUtils> utilities = mockStatic(ValidationUtils.class)) {
            utilities.when(() -> ValidationUtils.validateEditCashRequest(any(), any()))
                    .thenReturn(errors);

            when(accountService.getAccount("testuser", "USD"))
                    .thenReturn(Mono.just(account));

            webTestClient.post()
                    .uri("/api/testuser/cash")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(cashRequest)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(CashProcessResponse.class)
                    .value(response -> {
                        assertEquals("error", response.getStatus());
                        assertEquals(errors, response.getErrors());
                    });
        }
    }

    @Test
    void getUserCash_OkTest() {
        Account account = createAccount("USD", new BigDecimal("100.00"));

        when(accountService.getAccount("testuser", "USD"))
                .thenReturn(Mono.just(account));

        webTestClient.get()
                .uri("/api/testuser/account/USD")
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountDto.class)
                .value(dto -> {
                    assertEquals(Currency.USD, dto.getCurrency());
                    assertEquals(0, dto.getValue().compareTo(new BigDecimal("100.00")));
                    assertTrue(dto.isExists());
                });
    }

    @Test
    void getUserCash_NonExistTest() {
        when(accountService.getAccount("testuser", "GBP"))
                .thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/testuser/account/GBP")
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountDto.class)
                .value(dto -> {
                    assertFalse(dto.isExists());
                });
    }

    private Account createAccount(String currency, BigDecimal balance) {
        Account account = new Account();
        account.setCurrency(currency);
        account.setBalance(balance);
        return account;
    }

    private User createUser(String login, String name) {
        User user = new User();
        user.setLogin(login);
        user.setName(name);
        return user;
    }
}