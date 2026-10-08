package com.jsve.BancoIAS.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("account_transactions")
public class Transaction {

    @Id
    @Column("request_reference")
    private String requestReference;

    @Column("source_account_id")
    private String sourceAccountId;

    @Column("destination_account_id")
    private String destinationAccountId;

    private BigDecimal amount;

    @Column("processed_at")
    private LocalDateTime processedAt;
}