package com.jsve.BancoIAS.service.impl;

import com.jsve.BancoIAS.entity.Transaction;
import com.jsve.BancoIAS.exception.BusinessException;
import com.jsve.BancoIAS.repository.AccountRepository;
import com.jsve.BancoIAS.repository.TransactionRepository;
import com.jsve.BancoIAS.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
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
        return transactionRepository.existsById(transaction.getRequestReference())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException(
                                HttpStatus.CONFLICT,
                                "La transacción con referencia " + transaction.getRequestReference() + " ya existe."
                        ));
                    }

                    return validations(transaction)
                            .then(Mono.defer(() -> {
                                transaction.setProcessedAt(LocalDateTime.now());
                                transaction.setStatus("PROCESSED");
                                transaction.setReason(null);
                                processTransaction(transaction);

                                return entityTemplate.insert(Transaction.class)
                                        .using(transaction)
                                        .doOnNext(saved ->
                                                System.out.println("TRANSACCIÓN GUARDADA: " + saved.getRequestReference() + " | Estado: " + saved.getStatus())
                                        );
                            }))
                            .onErrorResume(BusinessException.class, exception -> {
                                transaction.setProcessedAt(LocalDateTime.now());
                                transaction.setStatus("REJECTED");
                                transaction.setReason(exception.getMessage());

                                return entityTemplate.insert(Transaction.class)
                                        .using(transaction)
                                        .doOnNext(saved ->
                                                System.out.println("RECHAZO GUARDADO: " + saved.getRequestReference()
                                        ));
                            });
                })
                .doOnError(error ->
                        System.err.println("ERROR AL PROCESAR TRANSACCIÓN: " + error.getMessage())
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

    private void processTransaction(Transaction transaction) {
        // Actualizar el saldo de la cuenta origen
        accountRepository.findById(transaction.getSourceAccountId())
            .flatMap(sourceAccount -> {
                sourceAccount.setBalance(sourceAccount.getBalance().subtract(transaction.getAmount()));
                return accountRepository.save(sourceAccount);
            }).subscribe();

        // Actualizar el saldo de la cuenta destino
        accountRepository.findById(transaction.getDestinationAccountId())
            .flatMap(destinationAccount -> {
                destinationAccount.setBalance(destinationAccount.getBalance().add(transaction.getAmount()));
                return accountRepository.save(destinationAccount);
            }).subscribe();
    }
}