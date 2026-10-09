package com.jsve.BancoIAS.service;

import com.jsve.BancoIAS.entity.Account;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountService {

    Flux<Account> getAll();

    Mono<Account> getById(String requestReference);
}
