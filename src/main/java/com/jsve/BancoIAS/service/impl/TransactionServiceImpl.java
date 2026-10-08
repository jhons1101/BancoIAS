package com.jsve.BancoIAS.service.impl;

import com.jsve.BancoIAS.entity.Transaction;
import com.jsve.BancoIAS.repository.TransactionRepository;
import com.jsve.BancoIAS.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

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
        return transactionRepository.save(transaction);
    }
}