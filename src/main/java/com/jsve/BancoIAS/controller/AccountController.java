package com.jsve.BancoIAS.controller;

import com.jsve.BancoIAS.entity.Account;
import com.jsve.BancoIAS.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public Flux<Account> getAll() {
        return accountService.getAll();
    }

    @GetMapping("/{requestReference}")
    public Mono<Account> getById(@PathVariable String requestReference) {
        return accountService.getById(requestReference);
    }
}
