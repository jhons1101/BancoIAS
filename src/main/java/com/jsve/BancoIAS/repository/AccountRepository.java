package com.jsve.BancoIAS.repository;

import com.jsve.BancoIAS.entity.Account;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Repository
public interface AccountRepository extends ReactiveCrudRepository<Account, String> {

    @Modifying
    @Query("""
        UPDATE accounts
        SET balance = balance - :amount
        WHERE account_id = :accountId
          AND status = 'ACTIVE'
          AND balance >= :amount
        """)
    Mono<Integer> debitIfSufficient(
            @Param("accountId") String accountId,
            @Param("amount") BigDecimal amount
    );

    @Modifying
    @Query("""
        UPDATE accounts
        SET balance = balance + :amount
        WHERE account_id = :accountId
          AND status = 'ACTIVE'
        """)
    Mono<Integer> creditIfActive(
            @Param("accountId") String accountId,
            @Param("amount") BigDecimal amount
    );
}
