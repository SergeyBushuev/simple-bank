package ru.yandex.exchange.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import ru.yandex.exchange.service.ExchangeRateService;
import ru.yandex.sharedlib.account.CurrencyDto;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ExchangeRatesController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/api/rates")
    public Flux<CurrencyDto> getCurrencies() {
        return exchangeRateService.getAllCurrentRates();
    }

}
