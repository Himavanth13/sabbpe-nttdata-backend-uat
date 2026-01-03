package com.sabbpe.nttdata.services;

import com.sabbpe.nttdata.dtos.TokenGenerationRequest;
import com.sabbpe.nttdata.enums.TransactionStatus;
import com.sabbpe.nttdata.models.MasterTransaction;
import com.sabbpe.nttdata.repositories.MasterTransactionRepository;
import com.sabbpe.nttdata.repositories.NttTransactionRepository;
import com.sabbpe.nttdata.utils.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionTokenService {

    private final ClientProfileService clientProfileService;
    private final NttTransactionRepository nttTransactionRepository;
    private final MasterTransactionRepository masterTransactionRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter TS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int TOKEN_VALIDITY_MINUTES = 15;

    @Transactional
    public String generateToken(TokenGenerationRequest request) throws Exception {

        Map<String, Object> keys = clientProfileService.getKeys(
                request.getTransactionUserId(),
                request.getTransactionMerchantId()
        );

        if (keys == null || keys.isEmpty()) {
            throw new IllegalStateException("No client_profile crypto keys found");
        }

        String aesKey = String.valueOf(keys.get("transactionAesKey"));
        String aesIv = String.valueOf(keys.get("transactionIv"));
        String password = String.valueOf(keys.get("transactionPassword"));

        LocalDateTime ldt = LocalDateTime.parse(request.getTransactionTimestamp(), TS_FORMATTER);
        String normalizedTs = ldt.format(TS_FORMATTER);

        String raw = request.getTransactionUserId()
                + request.getTransactionMerchantId()
                + password
                + normalizedTs;

        String encryptedToken = AESUtil.encrypt(raw, aesKey, aesIv);

        String internalTxnRef = "TXN"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        MasterTransaction masterTxn = MasterTransaction.builder()
                .orderReference(internalTxnRef)
                .transactionToken(encryptedToken)
                .transactionTimestamp(ldt)
                .transactionUserId(request.getTransactionUserId())
                .transactionMerchantId(request.getTransactionMerchantId())
                .clientId(request.getClientId())
                .amountRequested(BigDecimal.ZERO)
                .amountFinal(null)
                .currency("INR")
                .status(TransactionStatus.TOKEN_GENERATED)
                .processor("NTTDATA")
                .build();

        MasterTransaction savedMasterTxn = masterTransactionRepository.save(masterTxn);

        log.info("Token generated for client: {}, transaction ID: {}",
                request.getClientId(), masterTxn.getId());

        return encryptedToken;
    }
}
