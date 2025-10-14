package ru.yandex.accounts.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.accounts.model.User;
import ru.yandex.accounts.repository.UserRepository;
import ru.yandex.sharedlib.account.UserDto;
import ru.yandex.sharedlib.settings.EditPasswordRequest;
import ru.yandex.sharedlib.signup.SignupRequest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MeterRegistry metrics;
    @InjectMocks
    private UserService userService;

    private User existingUser;

    @BeforeEach
    void setup() {
        existingUser = User.builder()
                .id(1L)
                .login("user1")
                .name("Old Name")
                .password("oldpass")
                .birthdate(LocalDate.of(1990, 1, 1))
                .role("USER")
                .build();
    }

    @Test
    void registerUser_OkTest() {
        SignupRequest req = SignupRequest.builder()
                .login("newuser")
                .name("New User")
                .password("rawpass")
                .birthdate("1985-05-20")
                .build();

        when(passwordEncoder.encode("rawpass")).thenReturn("encoded");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        User result = userService.registerNewUser(req).block();
        assertNotNull(result);
        then(userRepository).should().save(captor.capture());
        User saved = captor.getValue();
        assertEquals("newuser", saved.getLogin());
        assertEquals("New User", saved.getName());
        assertEquals("encoded", saved.getPassword());
        assertEquals("USER", saved.getRole());
    }

    @Test
    void editPassword_UserNotFoundTest() {
        when(userRepository.findByLogin("nope")).thenReturn(Mono.empty());
        EditPasswordRequest req = new EditPasswordRequest();
        req.setPassword("x");
        req.setConfirmPassword("x");

        assertThrows(UsernameNotFoundException.class,
                () -> userService.editPassword("nope", req).block());
    }

    @Test
    void findByUsername_OkTest() {
        when(userRepository.findByLogin("user1")).thenReturn(Mono.just(existingUser));
        Counter mockCounter = mock(Counter.class);

        when(metrics.counter(
                eq("user_successful_logins"),
                eq("login"),
                anyString()
        )).thenReturn(mockCounter);

        UserDto dto = userService.findByUsername("user1").block();
        assertNotNull(dto);
        assertEquals(existingUser.getLogin(), dto.getLogin());
        assertEquals(existingUser.getName(), dto.getName());
        assertEquals(existingUser.getRole(), dto.getRole());
        assertEquals(existingUser.getBirthdate(), dto.getBirthdate());
    }

    @Test
    void findByUsername_NotFoundTest() {
        when(userRepository.findByLogin("nope")).thenReturn(Mono.empty());
        Counter mockCounter = mock(Counter.class);

        when(metrics.counter(
                eq("user_failed_logins"),
                eq("login"),
                anyString()
        )).thenReturn(mockCounter);
        assertThrows(UsernameNotFoundException.class,
                () -> userService.findByUsername("nope").block());
        verify(metrics, times(1)).counter(
                eq("user_failed_logins"),
                eq("login"),
                anyString()
        );
    }

    @Test
    void findAllUsers_OkTest() {
        User u2 = new User();
        when(userRepository.findAll()).thenReturn(Flux.just(existingUser, u2));

        List<User> list = userService.findAllUsers().collectList().block();
        assertNotNull(list);
        assertEquals(2, list.size());
    }
}
