package com.jsve.BancoIAS.service.impl;

import com.jsve.BancoIAS.entity.Transaction;
import com.jsve.BancoIAS.exception.BusinessException;
import com.jsve.BancoIAS.repository.AccountRepository;
import com.jsve.BancoIAS.repository.TransactionRepository;
import com.jsve.BancoIAS.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private final TransactionRepository transactionRepository;

    @Autowired
    private final AccountRepository accountRepository;

    private final R2dbcEntityTemplate entityTemplate;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Flux<Transaction> getAll() {
        return transactionRepository.findAll();
    }

    @Override
    public Mono<Transaction> getById(String requestReference) {
        return transactionRepository.findById(requestReference);
    }

    @Override
    public Mono<Transaction> save(Transaction transaction) {

        return transactionRepository
                .existsById(transaction.getRequestReference())
                .flatMap(exists -> {

                    if (exists) {
                        return Mono.error(new BusinessException(
                                HttpStatus.CONFLICT,
                                "La transacción con referencia " + transaction.getRequestReference() + " ya existe."
                        ));
                    }

                    return validations(transaction)
                            .then(Mono.defer(() ->
                                    transactionalOperator.transactional(
                                            debitSource(transaction)
                                                    .then(creditDestination(transaction))
                                                    .then(saveProcessedTransaction(transaction))
                                    )
                            ))
                            .onErrorResume(BusinessException.class,
                                    exception -> saveRejectedTransaction(
                                            transaction,
                                            exception.getMessage()
                                    )
                            );
                })
                .doOnError(error ->
                        System.err.println(
                                "ERROR AL PROCESAR TRANSACCIÓN: " + error.getMessage()
                        )
                );
    }

    private Mono<Void> validations(Transaction transaction) {

        // 1. El valor de la transaccion sea mayor que cero
        if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new BusinessException(
                HttpStatus.BAD_REQUEST, "El valor de la transferencia debe ser mayor que cero."
            ));
        }

        // 2. Las cuentas origen y destino sean diferentes
        if (
            transaction.getSourceAccountId() == null ||
            transaction.getDestinationAccountId() == null ||
            transaction.getSourceAccountId().equals(transaction.getDestinationAccountId())
        ) {
            return Mono.error(new BusinessException(
                HttpStatus.BAD_REQUEST, "La cuenta origen y destino deben ser diferentes."
            ));
        }

        // 3. la cuenta origen debe existir
        return accountRepository.findById(transaction.getSourceAccountId())
            .switchIfEmpty(Mono.error(new BusinessException(
                HttpStatus.NOT_FOUND, "La cuenta origen no existe."
            )))
            .flatMap(sourceAccount ->
                // 4. la cuenta destino debe existir
                accountRepository.findById(transaction.getDestinationAccountId())
                    .switchIfEmpty(Mono.error(new BusinessException(
                        HttpStatus.NOT_FOUND, "La cuenta destino no existe."
                    )))
                   .flatMap(destinationAccount -> {
                       // 5. el estado de la cuenta origen debe de estar activo
                       if (!"ACTIVE".equalsIgnoreCase(sourceAccount.getStatus())) {
                           return Mono.error(new BusinessException(
                               HttpStatus.UNPROCESSABLE_ENTITY,
                               "La cuenta origen no está habilitada para realizar transferencias."
                           ));
                       }
                       // 6. el saldo disponible de la cuenta origen debe ser mayor o igual al monto de la transacción
                       if (sourceAccount.getBalance() == null || sourceAccount.getBalance().compareTo(transaction.getAmount()) < 0) {
                           return Mono.error(new BusinessException(
                               HttpStatus.UNPROCESSABLE_ENTITY,
                               "La cuenta origen no tiene saldo suficiente para realizar la transferencia."
                           ));
                       }
                       return Mono.<Void>empty();
                   })
            );
    }

    private Mono<Void> debitSource(Transaction transaction) {

        return accountRepository.debitIfSufficient(
                        transaction.getSourceAccountId(),
                        transaction.getAmount()
                )
                .flatMap(rowsUpdated -> {
                    if (rowsUpdated == 1) {
                        return Mono.<Void>empty();
                    }

                    return accountRepository.findById(transaction.getSourceAccountId())
                            .switchIfEmpty(Mono.error(new BusinessException(
                                    HttpStatus.NOT_FOUND,
                                    "La cuenta origen no existe."
                            )))
                            .flatMap(sourceAccount -> {
                                if (!"ACTIVE".equalsIgnoreCase(sourceAccount.getStatus())) {
                                    return Mono.error(new BusinessException(
                                            HttpStatus.UNPROCESSABLE_ENTITY,
                                            "La cuenta origen no está habilitada para realizar transferencias."
                                    ));
                                }

                                if (sourceAccount.getBalance() == null
                                        || sourceAccount.getBalance().compareTo(transaction.getAmount()) < 0) {
                                    return Mono.error(new BusinessException(
                                            HttpStatus.UNPROCESSABLE_ENTITY,
                                            "La cuenta origen no tiene saldo suficiente para realizar la transferencia."
                                    ));
                                }

                                return Mono.error(new BusinessException(
                                        HttpStatus.CONFLICT,
                                        "No fue posible debitar la cuenta origen por una actualización concurrente."
                                ));
                            });
                });
    }

    private Mono<Void> creditDestination(Transaction transaction) {
        return accountRepository.creditIfActive(
                        transaction.getDestinationAccountId(),
                        transaction.getAmount()
                )
                .flatMap(rowsUpdated -> {
                    if (rowsUpdated == 1) {
                        return Mono.<Void>empty();
                    }

                    return Mono.error(new BusinessException(
                            HttpStatus.CONFLICT,
                            "No fue posible acreditar la cuenta destino."
                    ));
                });
    }

    private Mono<Transaction> saveRejectedTransaction(
            Transaction transaction,
            String reason
    ) {

        transaction.setProcessedAt(LocalDateTime.now());
        transaction.setStatus("REJECTED");
        transaction.setReason(reason);

        return entityTemplate.insert(Transaction.class)
                .using(transaction)
                .doOnNext(saved -> System.out.println(
                        "TRANSFERENCIA RECHAZADA: " + saved.getRequestReference() + " || Motivo: " + saved.getReason()
                ));
    }

    private Mono<Transaction> saveProcessedTransaction(Transaction transaction) {

        transaction.setProcessedAt(LocalDateTime.now());
        transaction.setStatus("PROCESSED");
        transaction.setReason(null);

        return entityTemplate.insert(Transaction.class)
                .using(transaction)
                .doOnNext(saved -> System.out.println(
                        "TRANSFERENCIA PROCESADA: " + saved.getRequestReference() + " | Importe: " + saved.getAmount()
                ));
    }
}