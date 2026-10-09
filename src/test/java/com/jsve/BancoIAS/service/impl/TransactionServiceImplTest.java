package com.jsve.BancoIAS.service.impl;

import com.jsve.BancoIAS.entity.Account;
import com.jsve.BancoIAS.entity.Transaction;
import com.jsve.BancoIAS.exception.BusinessException;
import com.jsve.BancoIAS.repository.AccountRepository;
import com.jsve.BancoIAS.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionalOperator transactionalOperator;

    private R2dbcEntityTemplate entityTemplate;
    private TransactionServiceImpl service;

    @BeforeEach
    void setUp() {

        entityTemplate = mock(R2dbcEntityTemplate.class, RETURNS_DEEP_STUBS);
        service = new TransactionServiceImpl(transactionRepository, accountRepository, entityTemplate, transactionalOperator);
    }

    @Test
    void getAllReturnsTransactions() {
        Transaction transaction = transaction();
        when(transactionRepository.findAll()).thenReturn(Flux.just(transaction));

        StepVerifier.create(service.getAll())
                .expectNext(transaction)
                .verifyComplete();
    }

    @Test
    void getByIdReturnsTransaction() {
        Transaction transaction = transaction();
        when(transactionRepository.findById("REF-1")).thenReturn(Mono.just(transaction));

        StepVerifier.create(service.getById("REF-1"))
                .expectNext(transaction)
                .verifyComplete();
    }

    @Test
    void getByIdReturnsEmptyWhenMissing() {
        when(transactionRepository.findById("REF-1")).thenReturn(Mono.empty());

        StepVerifier.create(service.getById("REF-1")).verifyComplete();
    }

    @Test
    void savesProcessedTransactionAndUpdatesBothAccounts() {
        stubTransactionalPipeline();
        Transaction transaction = transaction();
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));
        when(accountRepository.findById("SRC")).thenReturn(Mono.just(account("SRC", "ACTIVE", "500.00")));
        when(accountRepository.findById("DST")).thenReturn(Mono.just(account("DST", "ACTIVE", "100.00")));
        when(accountRepository.debitIfSufficient("SRC", transaction.getAmount())).thenReturn(Mono.just(1));
        when(accountRepository.creditIfActive("DST", transaction.getAmount())).thenReturn(Mono.just(1));

        StepVerifier.create(service.save(transaction))
                .assertNext(saved -> {
                    assertEquals("PROCESSED", saved.getStatus());
                    assertNull(saved.getReason());
                    assertEquals("REF-1", saved.getRequestReference());
                })
                .verifyComplete();

        verify(entityTemplate.insert(Transaction.class)).using(transaction);
    }

    @Test
    void rejectsDuplicateReferenceWithoutSaving() {
        Transaction transaction = transaction();
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(true));

        StepVerifier.create(service.save(transaction))
                .expectErrorSatisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertEquals(HttpStatus.CONFLICT, exception.getStatus());
                })
                .verify();

        verify(accountRepository, never()).debitIfSufficient(any(), any());
    }

    @Test
    void rejectsNullAmountAndPersistsReason() {
        stubInsertPipeline();
        Transaction transaction = transaction();
        transaction.setAmount(null);
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));

        assertRejected(transaction, "El valor de la transferencia debe ser mayor que cero.");
        verify(accountRepository, never()).findById(anyString());
    }

    @Test
    void rejectsZeroOrNegativeAmountAndPersistsReason() {
        stubInsertPipeline();
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));
        for (String amount : new String[]{"0", "-1"}) {
            Transaction transaction = transaction();
            transaction.setAmount(new BigDecimal(amount));

            assertRejected(transaction, "El valor de la transferencia debe ser mayor que cero.");
        }

        verify(accountRepository, never()).findById(anyString());
    }

    @Test
    void rejectsMissingOrSameAccountIds() {
        stubInsertPipeline();
        Transaction missingSource = transaction();
        missingSource.setSourceAccountId(null);
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));
        assertRejected(missingSource, "La cuenta origen y destino deben ser diferentes.");

        Transaction missingDestination = transaction();
        missingDestination.setDestinationAccountId(null);
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));
        assertRejected(missingDestination, "La cuenta origen y destino deben ser diferentes.");

        Transaction sameAccount = transaction();
        sameAccount.setDestinationAccountId("SRC");
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));
        assertRejected(sameAccount, "La cuenta origen y destino deben ser diferentes.");

        verify(accountRepository, never()).findById(anyString());
    }

    @Test
    void rejectsWhenSourceAccountDoesNotExist() {
        stubInsertPipeline();
        Transaction transaction = transaction();
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));
        when(accountRepository.findById("SRC")).thenReturn(Mono.empty());

        assertRejected(transaction, "La cuenta origen no existe.");
        verify(accountRepository, never()).findById("DST");
    }

    @Test
    void rejectsWhenDestinationAccountDoesNotExist() {
        stubInsertPipeline();
        Transaction transaction = transaction();
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));
        when(accountRepository.findById("SRC")).thenReturn(Mono.just(account("SRC", "ACTIVE", "500.00")));
        when(accountRepository.findById("DST")).thenReturn(Mono.empty());

        assertRejected(transaction, "La cuenta destino no existe.");
    }

    @Test
    void rejectsInactiveSourceAccount() {
        stubInsertPipeline();
        Transaction transaction = transaction();
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));
        when(accountRepository.findById("SRC")).thenReturn(Mono.just(account("SRC", "BLOCKED", "500.00")));
        when(accountRepository.findById("DST")).thenReturn(Mono.just(account("DST", "ACTIVE", "100.00")));

        assertRejected(transaction, "La cuenta origen no está habilitada para realizar transferencias.");
        verify(accountRepository, never()).debitIfSufficient(any(), any());
    }

    @Test
    void rejectsInsufficientOrNullSourceBalance() {
        stubInsertPipeline();
        Transaction transaction = transaction();
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));
        when(accountRepository.findById("SRC")).thenReturn(Mono.just(account("SRC", "ACTIVE", "50.00")));
        when(accountRepository.findById("DST")).thenReturn(Mono.just(account("DST", "ACTIVE", "100.00")));

        assertRejected(transaction, "La cuenta origen no tiene saldo suficiente para realizar la transferencia.");
        verify(accountRepository, never()).debitIfSufficient(any(), any());

        Transaction nullBalanceTransaction = transaction();
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));
        when(accountRepository.findById("SRC")).thenReturn(Mono.just(account("SRC", "ACTIVE", null)));
        when(accountRepository.findById("DST")).thenReturn(Mono.just(account("DST", "ACTIVE", "100.00")));

        assertRejected(nullBalanceTransaction, "La cuenta origen no tiene saldo suficiente para realizar la transferencia.");
    }

    @Test
    void concurrentInsufficientBalanceIsRejectedAfterAtomicDebitFails() {
        stubTransactionalPipeline();
        Transaction transaction = transaction();
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));
        when(accountRepository.findById("SRC"))
                .thenReturn(Mono.just(account("SRC", "ACTIVE", "500.00")))
                .thenReturn(Mono.just(account("SRC", "ACTIVE", "100.00")));
        when(accountRepository.findById("DST")).thenReturn(Mono.just(account("DST", "ACTIVE", "100.00")));
        when(accountRepository.debitIfSufficient("SRC", transaction.getAmount())).thenReturn(Mono.just(0));

        assertRejected(transaction, "La cuenta origen no tiene saldo suficiente para realizar la transferencia.");
    }

    @Test
    void rejectsUnexpectedDebitRaceAndFailedDestinationCredit() {
        stubTransactionalPipeline();
        Transaction debitRace = transaction();
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));
        when(accountRepository.findById("SRC"))
                .thenReturn(Mono.just(account("SRC", "ACTIVE", "500.00")))
                .thenReturn(Mono.just(account("SRC", "ACTIVE", "500.00")));
        when(accountRepository.findById("DST")).thenReturn(Mono.just(account("DST", "ACTIVE", "100.00")));
        when(accountRepository.debitIfSufficient("SRC", debitRace.getAmount())).thenReturn(Mono.just(0));

        assertRejected(debitRace, "No fue posible debitar la cuenta origen por una actualización concurrente.");

        Transaction failedCredit = transaction();
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));
        when(accountRepository.findById("SRC")).thenReturn(Mono.just(account("SRC", "ACTIVE", "500.00")));
        when(accountRepository.findById("DST")).thenReturn(Mono.just(account("DST", "ACTIVE", "100.00")));
        when(accountRepository.debitIfSufficient("SRC", failedCredit.getAmount())).thenReturn(Mono.just(1));
        when(accountRepository.creditIfActive("DST", failedCredit.getAmount())).thenReturn(Mono.just(0));

        assertRejected(failedCredit, "No fue posible acreditar la cuenta destino.");
    }

    @Test
    void propagatesPersistenceFailuresInsteadOfReportingSaved() {
        stubTransactionalPipeline();
        Transaction transaction = transaction();
        RuntimeException failure = new RuntimeException("database unavailable");
        when(transactionRepository.existsById("REF-1")).thenReturn(Mono.just(false));
        when(accountRepository.findById("SRC")).thenReturn(Mono.just(account("SRC", "ACTIVE", "500.00")));
        when(accountRepository.findById("DST")).thenReturn(Mono.just(account("DST", "ACTIVE", "100.00")));
        when(accountRepository.debitIfSufficient("SRC", transaction.getAmount())).thenReturn(Mono.just(1));
        when(accountRepository.creditIfActive("DST", transaction.getAmount())).thenReturn(Mono.just(1));
        when(entityTemplate.insert(Transaction.class).using(transaction)).thenReturn(Mono.error(failure));

        StepVerifier.create(service.save(transaction))
                .expectErrorMatches(error -> error == failure)
                .verify();
    }

    private void assertRejected(Transaction transaction, String expectedReason) {
        StepVerifier.create(service.save(transaction))
                .assertNext(saved -> {
                    assertEquals("REJECTED", saved.getStatus());
                    assertEquals(expectedReason, saved.getReason());
                    assertEquals(transaction.getRequestReference(), saved.getRequestReference());
                })
                .verifyComplete();
    }

    private void stubInsertPipeline() {
        when(entityTemplate.insert(Transaction.class).using(any(Transaction.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    private void stubTransactionalPipeline() {
        stubInsertPipeline();
        when(accountRepository.creditIfActive(anyString(), any()))
                .thenReturn(Mono.just(1));
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Transaction transaction() {
        return new Transaction("REF-1", "SRC", "DST", new BigDecimal("200.00"),
                null, null, null);
    }

    private Account account(String id, String status, String balance) {
        return new Account(id, "USR-01", status, balance == null ? null : new BigDecimal(balance));
    }
}
