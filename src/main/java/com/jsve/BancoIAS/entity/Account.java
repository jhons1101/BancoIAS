package com.jsve.BancoIAS.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("accounts")
public class Account {

    @Id
    @Column("account_id")
    private String accountId;

    @Column("customer_id")
    private String customerId;

    @Column("status")
    private String status;

    @Column("balance")
    private BigDecimal balance;
}