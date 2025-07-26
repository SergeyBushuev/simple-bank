package ru.yandex.exchange.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import ru.yandex.sharedlib.account.CurrencyDto;

import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final ConcurrentHashMap<String, Supplier<BigDecimal>> rates = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> ratesNames = new ConcurrentHashMap<>();

    private static final Supplier<BigDecimal> USD = () -> {
        Random rand = new Random();
        return BigDecimal.valueOf(rand.nextDouble(85.0, 96.0));
    };

    private static final Supplier<BigDecimal> BYN = () -> {
        Random rand = new Random();
        return BigDecimal.valueOf(rand.nextDouble(85.0, 96.0));
    };
    static {
        rates.put("BYN", BYN);
        rates.put("USD", USD);
        rates.put("RUB", () -> BigDecimal.ONE);
        ratesNames.put("USD", "Доллар");
        ratesNames.put("RUB", "Рубль");
        ratesNames.put("BYN", "Беларусский рубль");
    }

    public Flux<CurrencyDto> getAllCurrentRates() {
        return Flux.fromIterable(rates.keySet())
                .map(this::createKeyCurrency);
    }

    private CurrencyDto createKeyCurrency(String key) {
        CurrencyDto cur = new CurrencyDto();
        cur.setTitle(key);
        cur.setName(ratesNames.get(key));
        cur.setValue(rates.get(key).get());
        return cur;
    }

}
