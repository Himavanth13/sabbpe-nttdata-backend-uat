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
            client_id      AS clientId,
            client_email   AS clientEmail,
            client_mobile  AS clientMobile,
            ntt_userid     AS nttUserId,
            ntt_password   AS nttPassword,
            ntt_merchantid AS nttMerchantId
        FROM client_profile
        WHERE client_email  = :custEmail
          AND client_mobile = :custMobile
        """, nativeQuery = true)
    Map<String, Object> findNttMappingByCustomer(
            @Param("custEmail") String custEmail,
            @Param("custMobile") String custMobile
    );
}
