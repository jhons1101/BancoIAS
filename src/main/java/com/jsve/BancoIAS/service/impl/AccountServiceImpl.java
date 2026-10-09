package com.jsve.BancoIAS.service.impl;

import com.jsve.BancoIAS.entity.Account;
import com.jsve.BancoIAS.repository.AccountRepository;
import com.jsve.BancoIAS.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    @Autowired
    private final AccountRepository accountRepository;

    @Override
    public Flux<Account> getAll() {
        return accountRepository.findAll();
    }

    @Override
    public Mono<Account> getById(String requestReference) {
        return accountRepository.findById(requestReference);
    }
}
