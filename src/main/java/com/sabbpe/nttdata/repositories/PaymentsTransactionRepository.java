package com.sabbpe.nttdata.repositories;

import com.sabbpe.nttdata.models.PaymentsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentsTransactionRepository extends JpaRepository<PaymentsTransaction, String> {

    /**
     * merchTxnId → merchantTransactionId in the entity
     */
    Optional<PaymentsTransaction> findByMerchantTransactionId(String merchantTransactionId);


    /**
     * Equivalent to earlier:
     * findByTransactionIdAndMerchantOrderId()
     *
     * Here: merchTxnId + merchId
     */
    Optional<PaymentsTransaction> findByMerchantTransactionIdAndTransactionMerchantId(
            String merchantTransactionId,
            String transactionMerchantId
    );


    /**
     * Same as earlier: find all records by clientId
     */
    List<PaymentsTransaction> findByClientId(String clientId);


    /**
     * Equivalent to earlier:
     * findTopByTransactionTokenOrderByCreatedAtDesc()
     */
    Optional<PaymentsTransaction> findTopByTransactionTokenOrderByCreatedAtDesc(String transactionToken);


    /**
     * Optional extra: find last transaction for a merchant
     */
    Optional<PaymentsTransaction> findTopByTransactionMerchantIdOrderByCreatedAtDesc(String merchantId);


    /**
     * Optional search by status code
     */
    List<PaymentsTransaction> findByStatusCode(String statusCode);
}
