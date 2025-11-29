package com.sabbpe.nttdata.repositories;

import com.sabbpe.nttdata.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public interface TransactionRepository extends JpaRepository<Transaction, String> {


    Optional<Transaction> findByTransactionId(String transactionId);
    Optional<Transaction> findByTransactionIdAndMerchantOrderId(
            String transactionId,
            String merchantOrderId
    );

}
