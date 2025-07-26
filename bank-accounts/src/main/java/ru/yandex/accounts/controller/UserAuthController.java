package ru.yandex.accounts.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.yandex.accounts.service.UserService;
import ru.yandex.sharedlib.account.UserDto;

@RestController
@RequestMapping("/auth/users")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserService userService;

    @GetMapping("/{username}")
    public Mono<UserDto> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username);
    }

}
