package com.jsve.BancoIAS.controller;

import com.jsve.BancoIAS.entity.Account;
import com.jsve.BancoIAS.service.AccountService;
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
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    private AccountController controller;

    @BeforeEach
    void setUp() {
        controller = new AccountController(accountService);
    }

    @Test
    void getAllReturnsAccountsFromService() {
        Account account = account();
        when(accountService.getAll()).thenReturn(Flux.just(account));

        StepVerifier.create(controller.getAll())
                .expectNext(account)
                .verifyComplete();

        verify(accountService).getAll();
    }

    @Test
    void getByIdReturnsAccountFromService() {
        Account account = account();
        when(accountService.getById("CTA-0001")).thenReturn(Mono.just(account));

        StepVerifier.create(controller.getById("CTA-0001"))
                .expectNext(account)
                .verifyComplete();

        verify(accountService).getById("CTA-0001");
    }

    @Test
    void getByIdPropagatesServiceError() {
        RuntimeException failure = new RuntimeException("database unavailable");
        when(accountService.getById("CTA-0001")).thenReturn(Mono.error(failure));

        StepVerifier.create(controller.getById("CTA-0001"))
                .expectErrorMatches(error -> error == failure)
                .verify();
    }

    private Account account() {
        return new Account("CTA-0001", "USR-10", "ACTIVE", new BigDecimal("500000"));
    }
}
