package com.jsve.BancoIAS.repository;

import com.jsve.BancoIAS.entity.Account;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository
        extends ReactiveCrudRepository<Account, String> {
}
