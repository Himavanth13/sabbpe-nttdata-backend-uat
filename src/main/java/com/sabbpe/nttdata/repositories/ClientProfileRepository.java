package com.sabbpe.nttdata.repositories;

import com.sabbpe.nttdata.models.ClientProfile;
import com.sabbpe.nttdata.projection.ClientCryptoProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public interface ClientProfileRepository extends JpaRepository<ClientProfile, String> {

    @Query(value = """
        SELECT
            transaction_aes_key AS transactionAesKey,
            transaction_iv AS transactionIv,
            transaction_password AS transactionPassword
        FROM client_profile
        WHERE transaction_userid = :transactionUserId
          AND transaction_merchantid = :transactionMerchantId
        """, nativeQuery = true)
    public Map<String, Object> getKeys(
            @Param("transactionUserId") String transactionUserId,
            @Param("transactionMerchantId") String transactionMerchantId
    );

    @Query(value = """
    SELECT 
        transaction_userid AS transactionUserId,
        transaction_merchantid AS transactionMerchantId,
        transaction_password AS transactionPassword,
        transaction_timestamp AS transactionTimestamp,
        transaction_aes_key AS transactionAesKey,
        transaction_iv AS transactionIv
    FROM client_profile
    WHERE transaction_userid = :transactionUserId
      AND transaction_merchantid = :transactionMerchantId
      AND transaction_password = :transactionPassword
      AND transaction_timestamp = :transactionTimestamp
    LIMIT 1
    """, nativeQuery = true)
    Map<String, Object> findOneExactAsMap(
            @Param("transactionUserId") String transactionUserId,
            @Param("transactionMerchantId") String transactionMerchantId,
            @Param("transactionPassword") String transactionPassword,
            @Param("transactionTimestamp") String transactionTimestamp
    );


}
