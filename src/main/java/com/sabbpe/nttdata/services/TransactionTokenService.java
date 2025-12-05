package com.sabbpe.nttdata.services;

import com.sabbpe.nttdata.models.Transaction;
import com.sabbpe.nttdata.repositories.TransactionRepository;
import com.sabbpe.nttdata.services.ClientProfileService;
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

    public String encryptTransaction(
            String transactionUserId,
            String transactionMerchantId,
            String clientId,
            String transactionTimestamp) throws Exception {

        // Fetch AES keys
        Map<String, Object> keys = clientProfileService.getKeys(transactionUserId, transactionMerchantId);

        String aesKey = (String) keys.get("transactionAesKey");
        String aesIv = (String) keys.get("transactionIv");
        String password = (String) keys.get("transactionPassword");


        // Parse timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime ldt = LocalDateTime.parse(transactionTimestamp, formatter);
        // Prepare raw token string
        String raw = transactionUserId + transactionMerchantId + password + ldt;

        // Encrypt
        String encryptedToken = AESUtil.encrypt(raw, aesKey, aesIv);


        // Save Transaction
        Transaction txn = new Transaction();
        txn.setClientId(clientId);
        txn.setMerchantOrderId(transactionMerchantId); // FIXED: required for next stage
//        txn.setMerchantTransactionTimestamp(ldt);
//        txn.setTransactionToken(encryptedToken);

        transactionRepository.save(txn);

        return encryptedToken;
    }
}
