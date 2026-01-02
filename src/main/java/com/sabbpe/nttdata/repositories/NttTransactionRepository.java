package com.sabbpe.nttdata.repositories;

import com.sabbpe.nttdata.models.NttTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NttTransactionRepository extends JpaRepository<NttTransaction, String> {
}
