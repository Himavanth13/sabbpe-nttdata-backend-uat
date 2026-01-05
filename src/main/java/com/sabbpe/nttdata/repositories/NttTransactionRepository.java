package com.sabbpe.nttdata.repositories;

import com.sabbpe.nttdata.models.NttTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NttTransactionRepository extends JpaRepository<NttTransaction, String> {
    Optional<NttTransaction> findByMasterTransactionId(String masterTransactionId);
}
