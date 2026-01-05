package com.sabbpe.nttdata.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.nttdata.enums.TransactionStatus;
import com.sabbpe.nttdata.models.MasterTransaction;
import com.sabbpe.nttdata.models.NttTransaction;
import com.sabbpe.nttdata.repositories.MasterTransactionRepository;
import com.sabbpe.nttdata.repositories.NttTransactionRepository;
import com.sabbpe.nttdata.utils.NttCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionCallbackService {

    private final NttCrypto nttCrypto;
    private final MasterTransactionRepository masterTransactionRepository;
    private final NttTransactionRepository nttTransactionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public String processCallback(String encData) {
        String frontendUrl = null;
        String merchTxnId = null;
        String encryptedData = null;

        try {
            // Step 1: Decrypt callback data
            String decryptedJson = nttCrypto.decryptResponse(encData);
            log.info(" Callback received and decrypted");

            // Step 2: Parse callback JSON
            JsonNode root = objectMapper.readTree(decryptedJson);

            // Step 3: Extract critical fields
            String transactionToken = extractField(root, "udf6");
            frontendUrl = extractField(root, "udf7");
            encryptedData = extractField(root, "udf10");
            merchTxnId = root.path("payInstrument")
                    .path("merchDetails")
                    .path("merchTxnId")
                    .asText();
            String statusCode = extractStatusCode(root);
            Long atomTxnId = extractAtomTxnId(root);
            BigDecimal totalAmount = extractTotalAmount(root);

            log.info(" Processing callback | Token: {} | MerchTxnId: {} | Status: {}",
                    transactionToken, merchTxnId, statusCode);

            // Step 4: Find master transaction
            MasterTransaction masterTxn = masterTransactionRepository
                    .findByTransactionToken(transactionToken)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Transaction not found for token: " + transactionToken
                    ));

            //  Step 5: Check idempotency
            if (isCallbackAlreadyProcessed(masterTxn)) {
                log.warn(" Duplicate callback detected for txn: {}", masterTxn.getId());
                return buildRedirectUrl(frontendUrl, encryptedData, null);
            }

            // Step 6: Save/Update NttTransaction (JSON ONLY)
            saveOrUpdateNttTransaction(
                    masterTxn.getId(),
                    decryptedJson,
                    atomTxnId,
                    merchTxnId,
                    statusCode,
                    totalAmount
            );

            //Step 7: Update master transaction status
            updateMasterTransaction(masterTxn, statusCode, atomTxnId, totalAmount);

            log.info("Callback processed successfully for merchTxnId: {}", merchTxnId);

            // Step 8: Redirect to frontend
            return buildRedirectUrl(frontendUrl, encryptedData, null);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return buildRedirectUrl(frontendUrl, encryptedData, "invalid_transaction");

        } catch (Exception e) {
            log.error("❌ Callback processing failed", e);
            return buildRedirectUrl(frontendUrl, encryptedData, "callback_failed");
        }
    }

    /**
     * Check if callback already processed
     */
    private boolean isCallbackAlreadyProcessed(MasterTransaction masterTxn) {
        return masterTxn.getStatus() == TransactionStatus.SUCCESS ||
                masterTxn.getStatus() == TransactionStatus.FAILED;
    }

    /**
     * Save NttTransaction - ONLY JSON + minimal fields
     */
    private void saveOrUpdateNttTransaction(
            String masterTransactionId,
            String callbackJson,
            Long atomTxnId,
            String merchTxnId,
            String statusCode,
            BigDecimal totalAmount) {

        Optional<NttTransaction> existingOpt =
                nttTransactionRepository.findByMasterTransactionId(masterTransactionId);

        NttTransaction nttTxn = existingOpt.orElse(
                NttTransaction.builder().build()
        );

        // Set ONLY essential fields
        nttTxn.setMasterTransactionId(masterTransactionId);
        nttTxn.setMerchTxnId(merchTxnId);
        nttTxn.setAtomTxnId(atomTxnId);
        nttTxn.setGatewayStatusCode(statusCode);
        nttTxn.setTotalAmount(totalAmount);

        // Store ONLY the callback JSON
        nttTxn.setResponsePayload(callbackJson);

        nttTransactionRepository.save(nttTxn);
        log.info(" NttTransaction saved with callback JSON");
    }

    /**
     * Update master transaction status
     */
    private void updateMasterTransaction(
            MasterTransaction masterTxn,
            String statusCode,
            Long atomTxnId,
            BigDecimal totalAmount) {

        // Map gateway status to enum
        if ("OTS0000".equalsIgnoreCase(statusCode)) {
            masterTxn.setStatus(TransactionStatus.SUCCESS);
        } else if ("OTS0001".equalsIgnoreCase(statusCode)) {
            masterTxn.setStatus(TransactionStatus.FAILED);
        } else {
            masterTxn.setStatus(TransactionStatus.PENDING);
        }

        // Update final amount
        if (totalAmount != null) {
            masterTxn.setAmountFinal(totalAmount);
        }

        // Timestamp handled by @UpdateTimestamp on completedAt
        masterTransactionRepository.save(masterTxn);
        log.info(" Master transaction updated: {}", masterTxn.getStatus());
    }

    /**
     * Build redirect URL
     */
    private String buildRedirectUrl(String frontendUrl, String merchTxnId, String error) {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            frontendUrl = "https://giftvouchersuat.sabbpe.com/payment-result";
        }

        try {
            StringBuilder url = new StringBuilder("redirect:").append(frontendUrl);

            if (merchTxnId != null) {
                url.append("?txnId=")
                        .append(URLEncoder.encode(merchTxnId, StandardCharsets.UTF_8));
            }

            if (error != null) {
                url.append(merchTxnId != null ? "&" : "?")
                        .append("error=").append(error);
            }

            return url.toString();
        } catch (Exception e) {
            log.error("Error building redirect URL", e);
            return "redirect:" + frontendUrl;
        }
    }

    /**
     * Extract field from extras
     */
    private String extractField(JsonNode root, String udfField) {
        JsonNode node = root.path("payInstrument")
                .path("extras")
                .path(udfField);
        return node.isMissingNode() ? null : node.asText();
    }

    /**
     * Extract status code
     */
    private String extractStatusCode(JsonNode root) {
        return root.path("payInstrument")
                .path("responseDetails")
                .path("statusCode")
                .asText();
    }

    /**
     * Extract atom transaction ID
     */
    private Long extractAtomTxnId(JsonNode root) {
        JsonNode node = root.path("payInstrument")
                .path("payDetails")
                .path("atomTxnId");
        return node.isNumber() ? node.asLong() : null;
    }

    /**
     * Extract total amount
     */
    private BigDecimal extractTotalAmount(JsonNode root) {
        JsonNode node = root.path("payInstrument")
                .path("payDetails")
                .path("totalAmount");
        return node.isNumber() ? BigDecimal.valueOf(node.asDouble()) : null;
    }
}
