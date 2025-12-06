package com.sabbpe.nttdata.repositories;

import com.sabbpe.nttdata.models.PaymentsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentsTransactionRepository extends JpaRepository<PaymentsTransaction, String> {

    /**
     * Find by merchTxnId (unique constraint exists on this column)
     */
    Optional<PaymentsTransaction> findByMerchTxnId(String merchTxnId);

    /**
     * Find by merchTxnId + merchId
     */
    Optional<PaymentsTransaction> findByMerchTxnIdAndMerchId(String merchTxnId, String merchId);

    /**
     * Find all transactions for a client
     */
    List<PaymentsTransaction> findByClientId(String clientId);

    /**
     * Find most recent row containing the token from token generation
     */
    Optional<PaymentsTransaction> findTopByTransactionTokenOrderByCreatedAtDesc(String transactionToken);

    /**
     * Find most recent row for a merchant
     */
    Optional<PaymentsTransaction> findTopByMerchIdOrderByCreatedAtDesc(String merchId);

    /**
     * Find all transactions by status code
     */
    List<PaymentsTransaction> findByStatusCode(String statusCode);

    /**
     * Find by udf1 (useful when UDF1 = transaction UUID)
     */
    Optional<PaymentsTransaction> findByUdf1(String udf1);

    /**
     * Find by udf3 (udf3 is often token)
     */
    Optional<PaymentsTransaction> findByUdf3(String udf3);

    /**
     * Find latest by customer email + mobile (optional use case)
     */
    Optional<PaymentsTransaction> findTopByCustEmailAndCustMobileOrderByCreatedAtDesc(
            String custEmail, String custMobile
    );
}
