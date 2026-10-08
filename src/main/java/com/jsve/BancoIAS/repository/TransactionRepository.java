package com.jsve.BancoIAS.repository;

import org.springframework.stereotype.Repository;
import com.jsve.BancoIAS.entity.Transaction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

@Repository
public interface TransactionRepository extends ReactiveCrudRepository<Transaction, String> {
}
