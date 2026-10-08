package com.jsve.BancoIAS.service;

import com.jsve.BancoIAS.entity.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionService {

    Flux<Transaction> getAll();

    Mono<Transaction> getById(String requestReference);

    Mono<Transaction> save(Transaction transaction);
}