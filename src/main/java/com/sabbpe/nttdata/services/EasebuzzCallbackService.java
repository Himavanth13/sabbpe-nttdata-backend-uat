package com.sabbpe.nttdata.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.nttdata.enums.TransactionStatus;
import com.sabbpe.nttdata.models.EasebuzzTransaction;
import com.sabbpe.nttdata.models.MasterTransaction;
import com.sabbpe.nttdata.repositories.EasebuzzTransactionRepository;
import com.sabbpe.nttdata.repositories.MasterTransactionRepository;
import com.sabbpe.nttdata.utils.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EasebuzzCallbackService {
    private final EasebuzzTransactionRepository easebuzzTransactionRepository;
    private final MasterTransactionRepository masterTransactionRepository;
    private final ClientProfileService clientProfileService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public String processCallback(Map<String, String> payload) throws Exception {

        log.info("==================== EASEBUZZ CALLBACK ====================");
        log.info("Callback:\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload));

        String frontendUrl = null;
        String encryptedOrderNumber = null;

        try {
            // Step 1: Validate required fields
            validateRequiredFields(payload);

            // Step 2: Extract critical fields
            String txnid = payload.get("txnid");
            String status = payload.get("status").toUpperCase();
            String amount = payload.get("amount");
            encryptedOrderNumber = payload.get("udf3");
            String token = payload.get("udf1");

            log.info("Processing: txnid={} | status={} | amount={}", txnid, status, amount);

            // Step 3: Find master transaction by token
            MasterTransaction masterTxn = masterTransactionRepository
                    .findByTransactionToken(token)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Master transaction not found for token: " + token
                    ));

            log.info(" Found master transaction: {}", masterTxn.getId());

            EasebuzzTransaction easebuzzTxn = easebuzzTransactionRepository
                    .findByMasterTransactionId(masterTxn.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Easebuzz transaction not found for master txn: " + masterTxn.getId()
                    ));

            frontendUrl = easebuzzTxn.getFrontendUrl();
            log.info(" Frontend URL from database: {}", frontendUrl);

            // Step 4: Check idempotency - prevent duplicate processing
            if (isCallbackAlreadyProcessed(masterTxn)) {
                log.warn("⚠Duplicate callback detected for txn: {}", masterTxn.getId());
                return buildRedirectUrl(frontendUrl, encryptedOrderNumber, status, null);
            }

            // Step 5: Verify hash for security
            if (!verifyCallbackHash(payload)) {
                log.error("Hash verification FAILED for: {}", txnid);
                throw new SecurityException("Invalid callback hash");
            }

            log.info("Hash verified");

            // Step 6: Save/Update Easebuzz transaction - ONLY JSON + minimal fields
            String callbackJson = objectMapper.writeValueAsString(payload);
            saveOrUpdateEasebuzzTransaction(
                    masterTxn.getId(),
                    callbackJson
            );

            log.info(" Callback saved");

            // Step 7: Update master transaction status
            updateMasterTransaction(masterTxn, status, amount);

            log.info("==================== SUCCESS ====================");

            // Step 8: Redirect to frontend
            return buildRedirectUrl(frontendUrl, encryptedOrderNumber, status, null);

        } catch (IllegalArgumentException e) {
            log.error(" Validation error: {}", e.getMessage());
            return buildRedirectUrl(frontendUrl, encryptedOrderNumber, "error", "invalid_transaction");

        } catch (SecurityException e) {
            log.error(" Security error: {}", e.getMessage());
            return buildRedirectUrl(frontendUrl, encryptedOrderNumber, "error", "invalid_signature");

        } catch (Exception e) {
            log.error("Callback processing failed", e);
            return buildRedirectUrl(frontendUrl, encryptedOrderNumber, "error", "callback_processing_failed");
        }
    }

    /**
     * Validate required callback fields
     */
    private void validateRequiredFields(Map<String, String> payload) {
        String[] required = {"txnid", "status", "amount", "email", "hash", "phone", "udf1", "udf3"};

        for (String field : required) {
            if (!payload.containsKey(field) || payload.get(field) == null || payload.get(field).isBlank()) {
                throw new IllegalArgumentException("Missing required field: " + field);
            }
        }
    }

    /**
     * Check if callback already processed (idempotency)
     */
    private boolean isCallbackAlreadyProcessed(MasterTransaction masterTxn) {
        return masterTxn.getStatus() == TransactionStatus.SUCCESS ||
                masterTxn.getStatus() == TransactionStatus.FAILED ||
                masterTxn.getStatus() == TransactionStatus.CANCELLED;
    }

    /**
     * Verify callback hash for security
     */
    private boolean verifyCallbackHash(Map<String, String> payload) {
        try {
            String custEmail = payload.get("email");
            String custPhone = payload.get("phone");

            Map<String, Object> mapping = clientProfileService
                    .getEasebuzzMappingByCustomer(custEmail, custPhone);

            if (mapping == null || mapping.isEmpty()) {
                log.error(" Customer mapping not found");
                return false;
            }

            String merchantSalt = String.valueOf(mapping.get("easebuzzSalt"));

            // Build hash string (reverse order for callback)
            String hashString = String.join("|",
                    merchantSalt,
                    payload.getOrDefault("status", ""),
                    payload.getOrDefault("udf10", ""),
                    payload.getOrDefault("udf9", ""),
                    payload.getOrDefault("udf8", ""),
                    payload.getOrDefault("udf7", ""),
                    payload.getOrDefault("udf6", ""),
                    payload.getOrDefault("udf5", ""),
                    payload.getOrDefault("udf4", ""),
                    payload.getOrDefault("udf3", ""),
                    payload.getOrDefault("udf2", ""),
                    payload.getOrDefault("udf1", ""),
                    payload.getOrDefault("email", ""),
                    payload.getOrDefault("firstname", ""),
                    payload.getOrDefault("productinfo", ""),
                    payload.getOrDefault("amount", ""),
                    payload.getOrDefault("txnid", ""),
                    payload.getOrDefault("key", "")
            );

            String calculatedHash = HashUtil.sha512Hex(hashString);
            String receivedHash = payload.get("hash");

            log.info(" Hash Verification:");
            log.info("   Calculated: {}", calculatedHash);
            log.info("   Received: {}", receivedHash);
            log.info("   Match: {}", calculatedHash.equalsIgnoreCase(receivedHash));

            return calculatedHash.equalsIgnoreCase(receivedHash);

        } catch (Exception e) {
            log.error(" Hash verification error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Save/Update Easebuzz transaction - ONLY JSON + minimal fields
     */
    private void saveOrUpdateEasebuzzTransaction(
            String masterTransactionId,
            String callbackJson
    ) {

        Optional<EasebuzzTransaction> existingOpt =
                easebuzzTransactionRepository.findByMasterTransactionId(masterTransactionId);

        EasebuzzTransaction easebuzzTxn = existingOpt.orElse(
                EasebuzzTransaction.builder().build()
        );

        // Store ONLY the callback JSON
        easebuzzTxn.setResponsePayload(callbackJson);

        easebuzzTransactionRepository.save(easebuzzTxn);
        log.info("EasebuzzTransaction saved with callback JSON");
    }

    /**
     * Update master transaction status
     */
    private void updateMasterTransaction(
            MasterTransaction masterTxn,
            String status,
            String amount) {

        // Map Easebuzz status to enum
        switch (status.toLowerCase()) {
            case "success":
                masterTxn.setStatus(TransactionStatus.SUCCESS);
                log.info("Payment SUCCESS");
                break;

            case "failure":
                masterTxn.setStatus(TransactionStatus.FAILED);
                log.info("Payment FAILED");
                break;

            case "userCancelled":
                masterTxn.setStatus(TransactionStatus.CANCELLED);
                log.info("Payment CANCELLED");
                break;

            default:
                masterTxn.setStatus(TransactionStatus.PENDING);
                log.info("Payment PENDING");
        }

        // Update final amount
        if (amount != null && !amount.isBlank()) {
            try {
                masterTxn.setAmountFinal(new BigDecimal(amount));
            } catch (NumberFormatException e) {
                log.warn(" Invalid amount format: {}", amount);
            }
        }

        masterTransactionRepository.save(masterTxn);
        log.info("Master transaction updated: {}", masterTxn.getStatus());
    }

    /**
     * Build redirect URL with encrypted order number
     */
    private String buildRedirectUrl(
            String frontendUrl,
            String encryptedOrderNumber,
            String status,
            String error) {

        // Clean trailing slash
        if (frontendUrl.endsWith("/")) {
            frontendUrl = frontendUrl.substring(0, frontendUrl.length() - 1);
        }

        try {
            StringBuilder url = new StringBuilder(frontendUrl);
            url.append("/payment-result");
            url.append("?status=").append(status != null ? status : "unknown");

            // Add encrypted order number if available
            if (encryptedOrderNumber != null && !encryptedOrderNumber.isBlank()) {
                url.append("&txnid=").append(URLEncoder.encode(encryptedOrderNumber, StandardCharsets.UTF_8));
            }

            // Add error if present
            if (error != null) {
                url.append("&error=").append(URLEncoder.encode(error, StandardCharsets.UTF_8));
            }

            String finalUrl = url.toString();
            log.info("↪Redirect URL: {}", finalUrl);

            return finalUrl;

        } catch (Exception e) {
            log.error("Error building redirect URL", e);
            return frontendUrl + "/payment-result?status=error";
        }
    }
}
