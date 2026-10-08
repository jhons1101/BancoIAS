package com.jsve.BancoIAS.controller;

import com.jsve.BancoIAS.entity.Transaction;
import com.jsve.BancoIAS.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public Flux<Transaction> getAll() {
        return transactionService.getAll();
    }

    @GetMapping("/{requestReference}")
    public Mono<Transaction> getById(@PathVariable String requestReference) {
        return transactionService.getById(requestReference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Transaction> save(@RequestBody Transaction transaction) {
        return transactionService.save(transaction);
    }
}
