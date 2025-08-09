package ru.yandex.sharedlib.account;

import lombok.Getter;

@Getter
public enum Currency {

    RUB("Ruble"),
    BYN("Belorussian ruble"),
    USD("US Dollar"),;

    private final String title;

    Currency(String title) {
        this.title = title;
    }

}
