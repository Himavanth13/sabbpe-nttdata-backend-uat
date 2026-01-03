package com.sabbpe.nttdata.repositories;

import com.sabbpe.nttdata.models.EasebuzzTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EasebuzzTransactionRepository
        extends JpaRepository<EasebuzzTransaction, String> {

    Optional<EasebuzzTransaction> findByMasterTransactionIdAndStatus(
            String masterTransactionId,
            String status
    );

    Optional<EasebuzzTransaction> findByMasterTransactionId(String masterTransactionId);
}
