package com.jsve.BancoIAS.controller;

import com.jsve.BancoIAS.entity.Transaction;
import com.jsve.BancoIAS.service.TransactionService;
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
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    private TransactionController controller;

    @BeforeEach
    void setUp() {
        controller = new TransactionController(transactionService, null);
    }

    @Test
    void getAllReturnsTransactionsFromService() {

        Transaction transaction = transaction("REF-001");
        when(transactionService.getAll()).thenReturn(Flux.just(transaction));

        StepVerifier.create(controller.getAll())
                .expectNext(transaction)
                .verifyComplete();

        verify(transactionService).getAll();
    }

    @Test
    void getByIdReturnsTransactionFromService() {
        Transaction transaction = transaction("REF-001");
        when(transactionService.getById("REF-001")).thenReturn(Mono.just(transaction));

        StepVerifier.create(controller.getById("REF-001"))
                .expectNext(transaction)
                .verifyComplete();

        verify(transactionService).getById("REF-001");
    }

    @Test
    void getByIdPropagatesServiceError() {

        RuntimeException failure = new RuntimeException("database unavailable");
        when(transactionService.getById("REF-001")).thenReturn(Mono.error(failure));

        StepVerifier.create(controller.getById("REF-001"))
                .expectErrorMatches(error -> error == failure)
                .verify();
    }

    @Test
    void saveReturnsSavedTransaction() {

        Transaction transaction = transaction("REF-001");
        when(transactionService.save(transaction)).thenReturn(Mono.just(transaction));

        StepVerifier.create(controller.save(transaction))
                .expectNext(transaction)
                .verifyComplete();

        verify(transactionService).save(transaction);
    }

    private Transaction transaction(String reference) {
        return new Transaction(reference, "REF-001", "CTA-0001", new BigDecimal("10000"),
                "PROCESSED", null, null);
    }
}
