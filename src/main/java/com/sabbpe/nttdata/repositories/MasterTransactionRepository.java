package com.sabbpe.nttdata.repositories;

import com.sabbpe.nttdata.models.MasterTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MasterTransactionRepository extends JpaRepository<MasterTransaction, String> {

    Optional<MasterTransaction> findByTransactionToken(String transactionToken);

    Optional<MasterTransaction> findByOrderReference(String orderReference);

    // repositories/MasterTransactionRepository.java
    @Query(value = """
    SELECT * FROM master_transactions
    WHERE client_id = :clientId 
      AND transaction_timestamp = :timestamp
      AND processor = :processor
    ORDER BY initiated_at DESC
    LIMIT 1
    """, nativeQuery = true)
    Optional<MasterTransaction> findLatestByClientIdAndTransactionTimestampAndProcessor(
            @Param("clientId") String clientId,
            @Param("timestamp") LocalDateTime timestamp,
            @Param("processor") String processor
    );


    boolean existsByTransactionToken(String transactionToken);

}
