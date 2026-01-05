package com.sabbpe.nttdata.repositories;

import com.sabbpe.nttdata.models.ClientProfile;
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
    Map<String, Object> getKeys(
            @Param("transactionUserId") String transactionUserId,
            @Param("transactionMerchantId") String transactionMerchantId
    );

    @Query(value = """
        SELECT
            transaction_userid   AS transactionUserId,
            transaction_password AS transactionPassword,
            transaction_aes_key  AS transactionAesKey,
            transaction_iv       AS transactionIv,
            transaction_merchantid AS transactionMerchantId
        FROM client_profile
        WHERE client_id = :clientId
        """, nativeQuery = true)
    Map<String, Object> getCryptoByClientId(@Param("clientId") String clientId);

    @Query(value = """
    SELECT
        ctp.client_id as clientId,
        ctp.ntt_userid as nttUserId,
        ctp.ntt_password as nttPassword,
        ctp.ntt_merchantid as nttMerchantId
        FROM client_transaction_profile ctp
        JOIN client_profile cp ON ctp.client_id = cp.client_id
        WHERE cp.client_email = :email
          AND cp.client_mobile = :mobile
        LIMIT 1
        """, nativeQuery = true)
    Map<String, Object> findNttMappingByCustomer(
            @Param("email") String email,
            @Param("mobile") String mobile
    );

    @Query(value = """
    SELECT
        ctp.client_id as clientId,
        ctp.easebuzz_key as easebuzzKey,
        ctp.easebuzz_salt as easebuzzSalt
    FROM client_transaction_profile ctp
    JOIN client_profile cp ON ctp.client_id = cp.client_id
    WHERE cp.client_email = :email
      AND cp.client_mobile = :mobile
    LIMIT 1
    """, nativeQuery = true)
    Map<String, Object> findEasebuzzMappingByCustomer(
            @Param("email") String email,
            @Param("mobile") String mobile
    );

}
