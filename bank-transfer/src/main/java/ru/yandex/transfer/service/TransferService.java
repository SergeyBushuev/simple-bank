package ru.yandex.transfer.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import ru.yandex.sharedlib.account.AccountDto;
import ru.yandex.sharedlib.cash.CashAction;
import ru.yandex.sharedlib.cash.CashProcessResponse;
import ru.yandex.sharedlib.cash.CashRequest;
import ru.yandex.sharedlib.transfer.TransferRequest;
import ru.yandex.transfer.client.AccountsClient;
import ru.yandex.transfer.client.BlockersClient;
import ru.yandex.transfer.client.ConvertClient;
import ru.yandex.transfer.client.NotificationsClient;
import ru.yandex.transfer.exception.AccountNotFoundException;
import ru.yandex.transfer.exception.InsufficientFundsException;
import ru.yandex.transfer.exception.TransferException;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountsClient accountsClient;
    private final BlockersClient blockersClient;
    private final NotificationsClient notificationsClient;
    private final ConvertClient convertClient;

    private static final String SUCCESS_MESSAGE = "Transaction successful: ";
    private static final String FAIL_MESSAGE = "Transfer error: ";
    private static final String BLOCKED_MESSAGE = "Transaction blocked: ";
    private static final String COMPLETED = "completed";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public Mono<ResponseEntity<Void>> processTransfer(String login, TransferRequest transferRequest) {
        BigDecimal value = transferRequest.getValue();
        List<String> transferErrors = new ArrayList<>();
        List<String> transferOtherErrors = new ArrayList<>();

        return blockersClient.sendBlockerRequest(value)
                .flatMap(blocked -> {
                    if (blocked) {
                        notificationsClient.sendNotification(login, formatMessage(BLOCKED_MESSAGE, transferRequest)).subscribe();
                        handleErrors(BLOCKED_MESSAGE, login, transferRequest.getToLogin(), transferErrors, transferOtherErrors);
                        return redirectToMain(transferErrors, transferOtherErrors);
                    }

                    return Mono.zip(
                                    accountsClient.getAccount(login, transferRequest.getFromCurrency()),
                                    accountsClient.getAccount(transferRequest.getToLogin(), transferRequest.getToCurrency())
                            )
                            .flatMap(accounts -> accountsBalanceCheck(
                                    login,
                                    transferRequest,
                                    accounts,
                                    transferErrors,
                                    transferOtherErrors))
                            .flatMap(accounts -> convertClient.convertAmount(transferRequest)
                                    .flatMap(convertedAmount -> handleTransfer(
                                            login,
                                            transferRequest,
                                            convertedAmount,
                                            transferErrors,
                                            transferOtherErrors))
                                    .doOnSuccess(v -> {
                                        String message = formatMessage(SUCCESS_MESSAGE, transferRequest);
                                        notificationsClient.sendNotification(login, message).subscribe();
                                    }))
                            .onErrorResume(ex -> {
                                String errorMessage = formatMessage("Ошибка перевода: " + ex.getMessage(), transferRequest);
                                notificationsClient.sendNotification(login, errorMessage).subscribe();
                                return redirectToMain(transferErrors, transferOtherErrors);
                            });
                });
    }

    private Mono<Tuple2<AccountDto, AccountDto>> accountsBalanceCheck(String login,
                                                                      TransferRequest transferRequest,
                                                                      Tuple2<AccountDto, AccountDto> accounts,
                                                                      List<String> transferErrors,
                                                                      List<String> transferOtherErrors) {
        AccountDto fromAccount = accounts.getT1();
        AccountDto toAccount = accounts.getT2();

        if (!fromAccount.isExists() || !toAccount.isExists()) {
            String message = "Account not found";
            handleErrors(message, login, transferRequest.getToLogin(), transferErrors, transferOtherErrors);
            return Mono.error(new AccountNotFoundException(message));
        }

        if (fromAccount.getValue().compareTo(transferRequest.getValue()) < 0) {
            String message = "Insufficient funds";
            handleErrors(message, login, transferRequest.getToLogin(), transferErrors, transferOtherErrors);
            return Mono.error(new InsufficientFundsException(message));
        }
        return Mono.zip(Mono.just(fromAccount), Mono.just(toAccount));
    }

    private Mono<ResponseEntity<Void>> handleTransfer(String login,
                                                      TransferRequest transferRequest,
                                                      BigDecimal convertedAmount,
                                                      List<String> transferErrors,
                                                      List<String> transferOtherErrors) {
        CashRequest withdrawRequest = CashRequest.builder()
                .currency(transferRequest.getFromCurrency())
                .value(transferRequest.getValue().toString())
                .action(CashAction.GET)
                .build();

        return accountsClient.processCash(login, withdrawRequest)
                .flatMap(withdrawResponse -> {
                    Mono<ResponseEntity<Void>> responseMessage = handleResponseErrors
                            (login, transferRequest, withdrawResponse, transferErrors, transferOtherErrors);
                    if (responseMessage != null) return responseMessage;

                    CashRequest depositRequest = CashRequest.builder()
                            .currency(transferRequest.getToCurrency())
                            .value(convertedAmount.toString())
                            .action(CashAction.PUT)
                            .build();
                    return accountsClient.processCash(transferRequest.getToLogin(), depositRequest)
                            .flatMap(depositResponse -> {
                                Mono<ResponseEntity<Void>> message = handleResponseErrors
                                        (login, transferRequest, depositResponse, transferErrors, transferOtherErrors);
                                return (message != null) ? message :
                                        redirectToMain(transferErrors, transferOtherErrors);
                            });
                });
    }

    private Mono<ResponseEntity<Void>> handleResponseErrors(String login,
                                                            TransferRequest transferRequest,
                                                            CashProcessResponse depositResponse,
                                                            List<String> transferErrors,
                                                            List<String> transferOtherErrors) {
        if (!COMPLETED.equals(depositResponse.getStatus())) {
            String message = FAIL_MESSAGE +
                    String.join(", ", depositResponse.getErrors());
            handleErrors(message, login, transferRequest.getToLogin(), transferErrors, transferOtherErrors);
            return Mono.error(new TransferException(message));
        }
        return null;
    }

    private void handleErrors(String message,
                              String fromLogin,
                              String toLogin,
                              List<String> transferErrors,
                              List<String> transferOtherErrors) {
        if (fromLogin.equals(toLogin)) {
            transferErrors.add(message);
        } else {
            transferOtherErrors.add(message);
        }
    }

    private String formatMessage(String message, TransferRequest transferRequest) {
        String dateTime = LocalDateTime.now().format(FORMATTER);
        return String.format(dateTime + " " + message + " пользователю %s на сумму %s %s в %s",
                transferRequest.getToLogin(),
                transferRequest.getFromCurrency(),
                transferRequest.getValue(),
                transferRequest.getToCurrency()
        );
    }

    private Mono<ResponseEntity<Void>> redirectToMain(@Nullable List<String> transferErrors,
                                                      @Nullable List<String> transferOtherErrors) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/");

        if (transferErrors != null) {
            builder.queryParam("transferErrors", transferErrors);
        }
        if (transferOtherErrors != null) {
            builder.queryParam("transferOtherErrors", transferOtherErrors);
        }

        URI location = builder
                .build()
                .toUri();

        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                .location(location)
                .build());
    }
}
