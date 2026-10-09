package com.jsve.BancoIAS.service.impl;

import com.jsve.BancoIAS.entity.Account;
import com.jsve.BancoIAS.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountServiceImpl(accountRepository);
    }

    @Test
    void getAllReturnsAccounts() {
        Account account = account();
        when(accountRepository.findAll()).thenReturn(Flux.just(account));

        StepVerifier.create(service.getAll())
                .expectNext(account)
                .verifyComplete();

        verify(accountRepository).findAll();
    }

    @Test
    void getByIdReturnsAccount() {
        Account account = account();
        when(accountRepository.findById("CTA-0001")).thenReturn(Mono.just(account));

        StepVerifier.create(service.getById("CTA-0001"))
                .expectNext(account)
                .verifyComplete();

        verify(accountRepository).findById("CTA-0001");
    }

    @Test
    void getByIdReturnsEmptyWhenAccountDoesNotExist() {
        when(accountRepository.findById("CTA-0002")).thenReturn(Mono.empty());

        StepVerifier.create(service.getById("CTA-0002"))
                .verifyComplete();
    }

    private Account account() {
        return new Account("CTA-0001", "USR-10", "ACTIVE", new BigDecimal("500.00"));
    }
}
