package com.sabbpe.nttdata.services;

import com.sabbpe.nttdata.models.Transaction;
import com.sabbpe.nttdata.repositories.TransactionRepository;
import com.sabbpe.nttdata.utils.AESUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionTokenService {

    private final ClientProfileService clientProfileService;
    private final TransactionRepository transactionRepository;

    private static final DateTimeFormatter TS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String encryptTransaction(
            String transactionUserId,
            String transactionMerchantId,
            String clientId,
            String transactionTimestamp
    ) throws Exception {

        // 1) Fetch AES config from client_profile
        Map<String, Object> keys =
                clientProfileService.getKeys(transactionUserId, transactionMerchantId);

        if (keys == null || keys.isEmpty()) {
            throw new IllegalStateException("No client_profile crypto keys found for given user/merchant");
        }

        String aesKey    = String.valueOf(keys.get("transactionAesKey"));
        String aesIv     = String.valueOf(keys.get("transactionIv"));
        String password  = String.valueOf(keys.get("transactionPassword"));

        // 2) Normalize timestamp
        LocalDateTime ldt = LocalDateTime.parse(transactionTimestamp, TS_FORMATTER);
        String normalizedTs = ldt.format(TS_FORMATTER);

        // 3) Prepare raw string
        // MUST MATCH THE OTHER SIDE EXACTLY
        String raw = transactionUserId + transactionMerchantId + password + normalizedTs;

        // 4) Encrypt
        String encryptedToken = AESUtil.encrypt(raw, aesKey, aesIv);

        // 5) Persist transaction row
        Transaction txn = new Transaction();
        txn.setClientId(clientId);
        txn.setMerchantOrderId(transactionMerchantId);
        txn.setMerchantTransactionTimestamp(ldt);
        txn.setTransactionToken(encryptedToken);

        transactionRepository.save(txn);

        // 6) Return token
        return encryptedToken;
    }
}
