package ru.yandex.gateway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import ru.yandex.gateway.client.AccountsClient;
import ru.yandex.sharedlib.signup.SignupRequest;
import ru.yandex.sharedlib.signup.SignupResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(SignupController.class)
@WithMockUser
class SignupControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AccountsClient accountsClient;

    @MockBean
    private ReactiveAuthenticationManager authenticationManager;

    @MockBean
    private ServerSecurityContextRepository securityContextRepository;

    @BeforeEach
    void init() {
        this.webTestClient = webTestClient.mutateWith(csrf());
    }

    @Test
    void signupPage_OkTest() {
        webTestClient.get()
                .uri("/signup")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class);
    }

    @Test
    void register_OkTest() {
        SignupResponse response = new SignupResponse();
        response.setName("Bob");
        response.setLogin("bob");
        response.setPassword("pass");
        response.setBirthdate("1998-02-04");
        response.setErrors(null);

        when(accountsClient.signup(any(SignupRequest.class)))
                .thenReturn(Mono.just(response));

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("bob", "pass");
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(Mono.just(token));
        when(securityContextRepository.save(any(), any()))
                .thenReturn(Mono.empty());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("name", "Bob");
        body.add("login", "bob");
        body.add("password", "pass");
        body.add("birthdate", "1998-02-04");

        webTestClient.post()
                .uri("/signup")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/");
    }

    @Test
    void register_ErrorTest() {
        SignupResponse response = new SignupResponse();
        response.setName("Bob");
        response.setLogin("bob");
        response.setPassword("pass");
        response.setBirthdate("1998-02-04");
        response.setErrors(List.of("Some error"));

        when(accountsClient.signup(any(SignupRequest.class)))
                .thenReturn(Mono.just(response));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("name", "Bob");
        body.add("login", "bob");
        body.add("password", "pass");
        body.add("birthdate", "1998-02-04");

        webTestClient.post()
                .uri("/signup")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class);
    }
}
