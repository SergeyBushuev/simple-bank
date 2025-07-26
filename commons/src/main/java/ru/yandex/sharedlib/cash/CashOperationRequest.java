package ru.yandex.sharedlib.cash;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashOperationRequest {
    private String currency;
    private String value;
    private CashAction action;
}
