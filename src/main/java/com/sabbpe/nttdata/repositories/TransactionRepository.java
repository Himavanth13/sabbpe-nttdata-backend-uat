package com.sabbpe.nttdata.repositories;

import com.sabbpe.nttdata.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByTransactionIdAndMerchantOrderId(
            String transactionId,
            String merchantOrderId
    );


    List<Transaction> findByClientId(String clientId);

    Optional<Transaction> findTopByTransactionTokenOrderByCreatedAtDesc(String transactionToken);
}
