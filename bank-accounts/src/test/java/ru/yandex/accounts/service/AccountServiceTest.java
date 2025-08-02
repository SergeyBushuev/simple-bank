package ru.yandex.accounts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.accounts.exception.AccountNotFoundException;
import ru.yandex.accounts.model.Account;
import ru.yandex.accounts.model.User;
import ru.yandex.accounts.repository.AccountRepository;
import ru.yandex.accounts.repository.UserRepository;
import ru.yandex.sharedlib.cash.CashAction;
import ru.yandex.sharedlib.cash.CashRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private User testUser;

    @BeforeEach
    void setup() {
        testUser = new User();
        testUser.setId(1L);
    }

    @Test
    void accountInteraction_OkTest() {
        when(userRepository.findByLogin("user")).thenReturn(Mono.just(testUser));
        Account accUSD = Account.builder().userId(1L).currency("USD").balance(BigDecimal.ZERO).build();
        Account accEUR = Account.builder().userId(1L).currency("EUR").balance(BigDecimal.ZERO).build();
        when(accountRepository.findByUserId(1L)).thenReturn(Flux.just(accUSD, accEUR));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(accountRepository.deleteByUserIdAndCurrency(1L, "EUR")).thenReturn(Mono.empty());

        accountService.updateAccounts("user", List.of("USD", "GBP")).block();

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        then(accountRepository).should().save(captor.capture());
        Account saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals("GBP", saved.getCurrency());

        then(accountRepository).should().deleteByUserIdAndCurrency(1L, "EUR");
    }

    @Test
    void accountInteraction_UserNotFoundTest() {
        when(userRepository.findByLogin("unknown")).thenReturn(Mono.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> accountService.updateAccounts("unknown", List.of("USD")).block());
    }

    @Test
    void getAccounts_OkTest() {
        when(userRepository.findByLogin("user")).thenReturn(Mono.just(testUser));
        Account acc1 = Account.builder().userId(1L).currency("USD").balance(BigDecimal.ZERO).build();
        Account acc2 = Account.builder().userId(1L).currency("EUR").balance(BigDecimal.ZERO).build();
        when(accountRepository.findByUserId(1L)).thenReturn(Flux.just(acc1, acc2));

        List<Account> list = accountService.getUserAccounts("user").collectList().block();
        assertNotNull(list);
        assertEquals(2, list.size());
        assertTrue(list.contains(acc1));
        assertTrue(list.contains(acc2));
    }

    @Test
    void getAccounts_UserNotFoundTest() {
        when(userRepository.findByLogin("user")).thenReturn(Mono.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> accountService.getUserAccounts("user").collectList().block());
    }

    @Test
    void cashInteractions_OkTest() {
        when(userRepository.findByLogin("user")).thenReturn(Mono.just(testUser));
        Account acc = Account.builder().userId(1L).currency("USD").balance(BigDecimal.valueOf(100)).build();
        when(accountRepository.findByUserIdAndCurrency(1L, "USD")).thenReturn(Mono.just(acc));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        CashRequest request = CashRequest.builder()
                .currency("USD")
                .value("50")
                .action(CashAction.PUT)
                .build();

        accountService.updateCashBalance("user", request).block();

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        then(accountRepository).should().save(captor.capture());
        Account saved = captor.getValue();
        assertEquals(0, saved.getBalance().compareTo(BigDecimal.valueOf(150)));
    }

    @Test
    void cashInteractions_UserNotFoundTest() {
        when(userRepository.findByLogin("unknown")).thenReturn(Mono.empty());
        CashRequest request = CashRequest.builder()
                .currency("USD")
                .value("10")
                .action(CashAction.PUT)
                .build();

        assertThrows(UsernameNotFoundException.class,
                () -> accountService.updateCashBalance("unknown", request).block());
    }

    @Test
    void cashInteractions_AccountNotFoundTest() {
        when(userRepository.findByLogin("user")).thenReturn(Mono.just(testUser));
        when(accountRepository.findByUserIdAndCurrency(1L, "GBP")).thenReturn(Mono.empty());
        CashRequest request = CashRequest.builder()
                .currency("GBP")
                .value("20")
                .action(CashAction.PUT)
                .build();

        assertThrows(AccountNotFoundException.class,
                () -> accountService.updateCashBalance("user", request).block());
    }
}
