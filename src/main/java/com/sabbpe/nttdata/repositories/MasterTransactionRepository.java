package com.sabbpe.nttdata.repositories;

import com.sabbpe.nttdata.models.MasterTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MasterTransactionRepository extends JpaRepository<MasterTransaction, String> {

    Optional<MasterTransaction> findByTransactionToken(String transactionToken);

    Optional<MasterTransaction> findByOrderReference(String orderReference);

    boolean existsByTransactionToken(String transactionToken);
}
