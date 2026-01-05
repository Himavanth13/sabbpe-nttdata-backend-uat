package com.sabbpe.nttdata.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.nttdata.dtos.EasebuzzInitiateRequest;
import com.sabbpe.nttdata.enums.TransactionStatus;
import com.sabbpe.nttdata.models.EasebuzzTransaction;
import com.sabbpe.nttdata.models.MasterTransaction;
import com.sabbpe.nttdata.repositories.EasebuzzTransactionRepository;
import com.sabbpe.nttdata.repositories.MasterTransactionRepository;
import com.sabbpe.nttdata.utils.AESUtil;
import com.sabbpe.nttdata.utils.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EasebuzzTransactionService {

        private final RestTemplate restTemplate;
        private final MasterTransactionRepository masterTransactionRepository;
        private final EasebuzzTransactionRepository easebuzzTransactionRepository;
        private final ClientProfileService clientProfileService;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Value("${easebuzz.url.initiate}")
        private String initiateUrl;

        private static final DateTimeFormatter TS_FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        private MasterTransaction validateToken(String token) throws Exception {
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("Missing transaction token");
            }

            MasterTransaction masterTxn = masterTransactionRepository
                    .findByTransactionToken(token)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid or unknown transaction token"));

            if (!masterTxn.getProcessor().equalsIgnoreCase("EASEBUZZ")) {
                throw new IllegalArgumentException("Token is not for EASEBUZZ processor");
            }

            // Get crypto config for validation
            Map<String, Object> crypto = clientProfileService.getCryptoByClientId(masterTxn.getClientId());

            if (crypto == null || crypto.isEmpty()) {
                throw new IllegalStateException("No crypto config found for client: " + masterTxn.getClientId());
            }

            String transactionUserId = String.valueOf(crypto.get("transactionUserId"));
            String transactionPassword = String.valueOf(crypto.get("transactionPassword"));
            String aesKey = String.valueOf(crypto.get("transactionAesKey"));
            String aesIv = String.valueOf(crypto.get("transactionIv"));
            String transactionMerchantId = String.valueOf(crypto.get("transactionMerchantId"));

            log.info("key to decrypt: {}", aesKey);
            log.info("iv to decrypt: {}", aesIv);

            LocalDateTime ts = masterTxn.getTransactionTimestamp();
            if (ts == null) {
                throw new IllegalStateException("Missing merchant transaction timestamp");
            }

            // Check token expiry (15 minutes)
            long minutesPassed = Duration.between(ts, LocalDateTime.now()).toMinutes();
            if (minutesPassed >= 15) {
                throw new IllegalArgumentException("Transaction token expired");
            }

            // Validate token by decrypting and comparing
            String expectedRaw = transactionUserId
                    + transactionMerchantId
                    + transactionPassword
                    + ts.format(TS_FORMATTER)
                    + masterTxn.getProcessor();

            String decryptedRaw = AESUtil.decrypt(token, aesKey, aesIv);

            if (!expectedRaw.equals(decryptedRaw)) {
                throw new IllegalArgumentException("Invalid transaction token (payload mismatch)");
            }

            log.info("Token validated for client_id={} merchantId={}",
                    masterTxn.getClientId(), transactionMerchantId);

            return masterTxn;
        }

        /**
         * Get Easebuzz credentials from DB only
         */
        private String[] getEasebuzzCredentials(String custEmail, String custMobile) {

            Map<String, Object> mapping = clientProfileService.getEasebuzzMappingByCustomer(custEmail, custMobile);

            if (mapping == null || mapping.isEmpty()) {
                throw new IllegalArgumentException("Customer not found: " + custEmail);
            }

            String dbKey = String.valueOf(mapping.get("easebuzzKey"));
            String dbSalt = String.valueOf(mapping.get("easebuzzSalt"));

            if (dbKey == null || dbKey.equals("null") || dbKey.isBlank()) {
                throw new IllegalStateException("Easebuzz credentials not configured for customer: " + custEmail);
            }

            log.info("Using DB Easebuzz credentials for: {}", custEmail);
            return new String[]{dbKey, dbSalt};
        }

        /**
         * Initiate Easebuzz payment
         */

        @Transactional
        public Map<String, Object> initiatePayment(EasebuzzInitiateRequest request) throws JsonProcessingException {

            log.info(" Easebuzz initiate request:\n{}",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));

            try {
                // ==============================
                // 1. Extract fields
                // ==============================

                String email = request.getEmail();
                String phone = request.getPhone();
                String token = request.getUdf1();

                // ==============================
                // 2. Validate token
                // ==============================

                MasterTransaction masterTxn = validateToken(token);

                String frontendUrl = request.getUdf2();

                request.setUdf2("");

                Optional<EasebuzzTransaction> existingPayment = easebuzzTransactionRepository
                        .findByMasterTransactionIdAndStatus(
                                masterTxn.getId(),
                                "INITIATED"
                        );

                if (existingPayment.isPresent()) {
                    EasebuzzTransaction existing = existingPayment.get();

                    log.warn(" Payment already initiated for master transaction: {}", masterTxn.getId());

                    Map<String, Object> duplicateResponse = Map.of(
                            "paymentUrl", existing.getPaymentUrl(),
                            "status", "success",
                            "message", "Payment already initiated",
                            "accessKey", existing.getAccessKey(),
                            "txnid", existing.getTxnid(),
                            "masterTransactionId", masterTxn.getId()
                    );

                    existing.setResponsePayload(objectMapper.writeValueAsString(duplicateResponse));
                    easebuzzTransactionRepository.save(existing);

                    return duplicateResponse;
                }

                // ==============================
                // 3. Get credentials from DB
                // ==============================
                String[] credentials = getEasebuzzCredentials(email, phone);
                String finalKey = credentials[0];
                String finalSalt = credentials[1];

                log.info(" Using Easebuzz key: {}", finalKey);

                // ==============================
                // 4. Update master transaction
                // ==============================

                BigDecimal amount = new BigDecimal(request.getAmount());
                masterTxn.setAmountRequested(amount);
                masterTxn.setStatus(TransactionStatus.INITIATED);
                masterTransactionRepository.save(masterTxn);

                // ==============================
                // 5. Generate hash
                // ==============================

                String hashPlain = String.join("|",
                        finalKey,
                        request.getTxnid(),
                        request.getAmount(),
                        request.getProductinfo(),
                        request.getFirstname(),
                        request.getEmail(),
                        request.getUdf1() != null ? request.getUdf1() : "",
                        request.getUdf2() != null ? request.getUdf2() : "",
                        request.getUdf3() != null ? request.getUdf3() : "",
                        request.getUdf4() != null ? request.getUdf4() : "",
                        request.getUdf5() != null ? request.getUdf5() : "",
                        request.getUdf6() != null ? request.getUdf6() : "",
                        request.getUdf7() != null ? request.getUdf7() : "",
                        request.getUdf8() != null ? request.getUdf8() : "",
                        request.getUdf9() != null ? request.getUdf9() : "",
                        request.getUdf10() != null ? request.getUdf10() : "",
                        finalSalt
                );

                String hash = HashUtil.sha512Hex(hashPlain);

                log.info("🔐 Hash plain text: {}", hashPlain);
                log.info(" Generated hash for txnid: {}", request.getTxnid());

                // ==============================
                // 6. Prepare request body
                // ==============================

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("key", finalKey);
                body.add("txnid", request.getTxnid());
                body.add("amount", request.getAmount());
                body.add("productinfo", request.getProductinfo());
                body.add("firstname", request.getFirstname());
                body.add("phone", request.getPhone());
                body.add("email", request.getEmail());
                body.add("surl", request.getSurl());
                body.add("furl", request.getFurl());
                body.add("hash", hash);

                body.add("udf1", request.getUdf1());
                body.add("udf2", request.getUdf2());
                body.add("udf3", request.getUdf3());
                body.add("udf4", request.getUdf4());
                body.add("udf5", request.getUdf5());
                body.add("udf6", request.getUdf6());
                body.add("udf7", request.getUdf7());
                body.add("udf8", request.getUdf8());
                body.add("udf9", request.getUdf9());
                body.add("udf10", request.getUdf10());


                if (request.getAddress1() != null && !request.getAddress1().isEmpty())
                    body.add("address1", request.getAddress1());
                if (request.getAddress2() != null && !request.getAddress2().isEmpty())
                    body.add("address2", request.getAddress2());
                if (request.getCity() != null && !request.getCity().isEmpty())
                    body.add("city", request.getCity());
                if (request.getState() != null && !request.getState().isEmpty())
                    body.add("state", request.getState());
                if (request.getCountry() != null && !request.getCountry().isEmpty())
                    body.add("country", request.getCountry());
                if (request.getZipcode() != null && !request.getZipcode().isEmpty())
                    body.add("zipcode", request.getZipcode());

                if (request.getShowPaymentMode() != null && !request.getShowPaymentMode().isEmpty())
                    body.add("show_payment_mode", request.getShowPaymentMode());

                if (request.getSplitPayments() != null) {
                    body.add("split_payments", objectMapper.writeValueAsString(request.getSplitPayments()));
                }

                if ("SEAMLESS".equalsIgnoreCase(request.getRequestFlow())) {
                    body.add("request_flow", "SEAMLESS");
                }

                if (request.getSubMerchantId() != null && !request.getSubMerchantId().isEmpty())
                    body.add("sub_merchant_id", request.getSubMerchantId());
                if (request.getPaymentCategory() != null && !request.getPaymentCategory().isEmpty())
                    body.add("payment_category", request.getPaymentCategory());
                if (request.getAccountNo() != null && !request.getAccountNo().isEmpty())
                    body.add("account_no", request.getAccountNo());
                if (request.getIfsc() != null && !request.getIfsc().isEmpty())
                    body.add("ifsc", request.getIfsc());
                if (request.getUniqueId() != null && !request.getUniqueId().isEmpty())
                    body.add("unique_id", request.getUniqueId());


                // ==============================
                // 7. Save request (trigger will parse)
                // ==============================

                String requestJson = objectMapper.writeValueAsString(request);

                EasebuzzTransaction easebuzzTxn = EasebuzzTransaction.builder()
                        .masterTransactionId(masterTxn.getId())
                        .frontendUrl(frontendUrl)
                        .requestPayload(requestJson)
                        .build();


                easebuzzTransactionRepository.saveAndFlush(easebuzzTxn);
                log.info(" Easebuzz transaction saved: ID={}", easebuzzTxn.getId());

                // ==============================
                // 8. Call Easebuzz API
                // ==============================

                HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

                log.info(" Calling Easebuzz API: {}", initiateUrl);

                ResponseEntity<Map> response = restTemplate.exchange(
                        initiateUrl,
                        HttpMethod.POST,
                        entity,
                        Map.class
                );

                if (response.getBody() == null) {
                    log.error(" Invalid response from Easebuzz");
                    masterTxn.setStatus(TransactionStatus.FAILED);
                    masterTransactionRepository.save(masterTxn);
                    throw new RuntimeException("Invalid response from Easebuzz");
                }

                // ==============================
                // 9. Save response
                // ==============================

                String responseJson = objectMapper.writeValueAsString(response.getBody());
                Map<String, Object> responseData = response.getBody();

                Integer status = (Integer) responseData.get("status");
                String accessKey = (String) responseData.get("data");

                if (status != null && status == 1 && accessKey != null) {

                    String paymentUrl = initiateUrl.contains("testpay")
                            ? "https://testpay.easebuzz.in/pay/" + accessKey
                            : "https://pay.easebuzz.in/pay/" + accessKey;

                    Map<String, Object> cleanResponse = Map.of(
                            "paymentUrl", paymentUrl,
                            "status", "INITIATED",
                            "message", "Payment initiated successfully",
                            "accessKey", accessKey,
                            "txnid", request.getTxnid(),
                            "masterTransactionId", masterTxn.getId()
                    );

                    easebuzzTxn.setResponsePayload(objectMapper.writeValueAsString(cleanResponse));
                    easebuzzTransactionRepository.save(easebuzzTxn);

                    masterTxn.setStatus(TransactionStatus.PENDING);
                    masterTransactionRepository.save(masterTxn);

                    log.info("Easebuzz payment initiated | Master: {} | Easebuzz: {} | URL: {}",
                            masterTxn.getId(), easebuzzTxn.getId(), paymentUrl);

                    return cleanResponse;

                } else {
                    String errorDesc = responseData.get("error_desc") != null
                            ? String.valueOf(responseData.get("error_desc"))
                            : "Payment initiation failed";

                    Map<String, Object> errorResponse = Map.of(
                            "status", "failed",
                            "message", errorDesc,
                            "txnid", request.getTxnid(),
                            "masterTransactionId", masterTxn.getId()
                    );

                    easebuzzTxn.setResponsePayload(objectMapper.writeValueAsString(errorResponse));
                    easebuzzTransactionRepository.save(easebuzzTxn);

                    masterTxn.setStatus(TransactionStatus.FAILED);
                    masterTransactionRepository.save(masterTxn);

                    throw new RuntimeException("Easebuzz initiation failed: " + errorDesc);
                }

            } catch (Exception e) {
                log.error(" Payment failed: {}", e.getMessage(), e);
                throw new RuntimeException("Payment initiation failed: " + e.getMessage(), e);
            }
        }

}
