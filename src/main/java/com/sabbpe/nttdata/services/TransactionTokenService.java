package com.sabbpe.nttdata.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.nttdata.models.PaymentsTransaction;
import com.sabbpe.nttdata.repositories.PaymentsTransactionRepository;
import com.sabbpe.nttdata.utils.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionTokenService {

    private final ClientProfileService clientProfileService;
    private final PaymentsTransactionRepository paymentsTransactionRepository;

    private static final DateTimeFormatter TS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String encryptTransaction(
            Map<String,Object> body,
            String transactionUserId,
            String transactionMerchantId,   // <-- merchTxnId
            String clientId,
            String transactionTimestamp
    ) throws Exception {

        Map<String, Object> keys =
                clientProfileService.getKeys(transactionUserId, transactionMerchantId);

        if (keys == null || keys.isEmpty()) {
            throw new IllegalStateException("No client_profile crypto keys found for given user/merchant");
        }

        String aesKey    = String.valueOf(keys.get("transactionAesKey"));
        String aesIv     = String.valueOf(keys.get("transactionIv"));
        String password  = String.valueOf(keys.get("transactionPassword"));
        log.info("key to encrypt : {}",aesKey);
        log.info("iv to encrypt : {}",aesIv);
        LocalDateTime ldt = LocalDateTime.parse(transactionTimestamp, TS_FORMATTER);
        String normalizedTs = ldt.format(TS_FORMATTER);

        // RAW STRING MUST MATCH VALIDATOR
        String raw = transactionUserId + transactionMerchantId + password + normalizedTs;

        // Encrypt token
        String encryptedToken = AESUtil.encrypt(raw, aesKey, aesIv);

        body.put("transaction_token",encryptedToken);
        // -------------------------------
        // SAVE INTO payments_transactions
        // -------------------------------
        PaymentsTransaction txn = new PaymentsTransaction();

        txn.setRequestMetadata(new ObjectMapper().writeValueAsString(body));

        paymentsTransactionRepository.save(txn);



        log.info("generate token request payload : {}",
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(body));
        log.info("data : {}",txn);
        return encryptedToken;
    }
}
