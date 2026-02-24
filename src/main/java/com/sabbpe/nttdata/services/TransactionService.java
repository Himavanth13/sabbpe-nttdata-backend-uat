package com.sabbpe.nttdata.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sabbpe.nttdata.dtos.TransactionRequest;
import com.sabbpe.nttdata.dtos.TransactionSuccessResponse;
import com.sabbpe.nttdata.enums.TransactionStatus;
import com.sabbpe.nttdata.models.MasterTransaction;
import com.sabbpe.nttdata.models.NttTransaction;
import com.sabbpe.nttdata.repositories.MasterTransactionRepository;
import com.sabbpe.nttdata.repositories.NttTransactionRepository;
import com.sabbpe.nttdata.utils.AESUtil;
import com.sabbpe.nttdata.utils.NttCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final RestTemplate restTemplate;
    private final NttCrypto nttCrypto;
    private final MasterTransactionRepository masterTransactionRepository;
    private final NttTransactionRepository nttTransactionRepository;
    private final ClientProfileService clientProfileService;
    private final ObjectMapper objectMapper;

    @Value("${ndps.auth-url}")
    private String authUrl;

    private static final DateTimeFormatter TS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Validate transaction token
     */
    private void validateTransactionToken(String token, String clientId) throws Exception {

        MasterTransaction masterTxn = masterTransactionRepository
                .findByTransactionToken(token)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid or unknown transaction token"));

        if (!masterTxn.getClientId().equals(clientId)) {
            throw new IllegalArgumentException("Token does not belong to this client");
        }

        if (!masterTxn.getProcessor().equalsIgnoreCase("NTTDATA")) {
            throw new IllegalArgumentException("Token is not for NTTDATA processor");
        }

        Map<String, Object> crypto = clientProfileService.getCryptoByClientId(clientId);

        if (crypto == null || crypto.isEmpty()) {
            throw new IllegalStateException("No crypto config found for client: " + clientId);
        }

        String transactionUserId = String.valueOf(crypto.get("transactionUserId"));
        String transactionPassword = String.valueOf(crypto.get("transactionPassword"));
        String aesKey = String.valueOf(crypto.get("transactionAesKey"));
        String aesIv = String.valueOf(crypto.get("transactionIv"));
        String transactionMerchantId = String.valueOf(crypto.get("transactionMerchantId"));

        LocalDateTime ts = masterTxn.getTransactionTimestamp();

        if (ts == null) {
            throw new IllegalStateException("Missing merchant transaction timestamp");
        }

        long minutesPassed = Duration.between(ts, LocalDateTime.now()).toMinutes();
        if (minutesPassed >= 15) {
            throw new IllegalArgumentException("Transaction token expired");
        }

        String expectedRaw = transactionUserId
                + transactionMerchantId
                + transactionPassword
                + ts.format(TS_FORMATTER)
                + masterTxn.getProcessor();

        String decryptedRaw = AESUtil.decrypt(token, aesKey, aesIv);

        if (!expectedRaw.equals(decryptedRaw)) {
            throw new IllegalArgumentException("Invalid transaction token (payload mismatch)");
        }

        log.info("Transaction token validated for client_id={} merchantId={}",
                clientId, transactionMerchantId);
    }

    /**
     * Initiate payment with new architecture
     */
    @Transactional
    public TransactionSuccessResponse initiate(TransactionRequest request)
            throws JsonProcessingException {

        log.info("Incoming initiation request:\n{}",
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));

        try {
            String custEmail = request.getPayInstrument().getCustDetails().getCustEmail();
            String custMobile = request.getPayInstrument().getCustDetails().getCustMobile();

            if (custEmail == null || custEmail.isBlank()
                    || custMobile == null || custMobile.isBlank()) {
                throw new IllegalArgumentException("Customer email or mobile missing");
            }

            Map<String, Object> nttMapping =
                    clientProfileService.getNttMappingByCustomer(custEmail, custMobile);

            if (nttMapping == null || nttMapping.isEmpty()) {
                throw new IllegalArgumentException("Customer not mapped to client_profile");
            }

            String clientId = String.valueOf(nttMapping.get("clientId"));
            String nttUserId = String.valueOf(nttMapping.get("nttUserId"));
            String nttPassword = String.valueOf(nttMapping.get("nttPassword"));
            String nttMerchantId = String.valueOf(nttMapping.get("nttMerchantId"));

            if (nttUserId == null || nttUserId.isBlank()
                    || nttPassword == null || nttPassword.isBlank()
                    || nttMerchantId == null || nttMerchantId.isBlank()) {
                throw new IllegalStateException("NDPS credentials missing in client_profile");
            }

            String token = request.getPayInstrument().getExtras().getUdf6();
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("Missing transaction token in udf6");
            }

            validateTransactionToken(token, clientId);

            MasterTransaction masterTxn = masterTransactionRepository
                    .findByTransactionToken(token)
                    .orElseThrow(() -> new IllegalArgumentException("Token not found"));

            BigDecimal amount = BigDecimal.valueOf(
                    request.getPayInstrument().getPayDetails().getAmount()
            );

            masterTxn.setAmountRequested(amount);
            masterTxn.setStatus(TransactionStatus.INITIATED);
            masterTransactionRepository.save(masterTxn);

            request.getPayInstrument().getMerchDetails().setUserId(nttUserId);
            request.getPayInstrument().getMerchDetails().setMerchId(nttMerchantId);
            request.getPayInstrument().getMerchDetails().setPassword(nttPassword);

            String requestJson = objectMapper.writeValueAsString(request);
            log.info("Request with NTT credentials:\n{}",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));

            NttTransaction nttTxn = NttTransaction.builder()
                    .masterTransactionId(masterTxn.getId())
                    .requestPayload(requestJson)
                    .build();

            nttTransactionRepository.save(nttTxn);
            log.info("NTT transaction created: ID={} (trigger will populate columns)", nttTxn.getId());

            String encData = nttCrypto.encryptRequest(request);

            String form = "encData=" + URLEncoder.encode(encData, StandardCharsets.UTF_8)
                    + "&merchId=" + URLEncoder.encode(nttMerchantId, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<String> response = restTemplate.exchange(
                    authUrl, HttpMethod.POST, new HttpEntity<>(form, headers), String.class);

            if (response.getBody() == null || !response.getBody().contains("encData=")) {
                log.error("Invalid response from NTT Data: {}", response.getBody());
                masterTxn.setStatus(TransactionStatus.FAILED);
                masterTransactionRepository.save(masterTxn);
                throw new RuntimeException("Invalid response from payment gateway");
            }

            String encryptedResponse = response.getBody()
                    .substring(response.getBody().indexOf("encData=") + 8);

            TransactionSuccessResponse decrypted = nttCrypto.decryptResponse(
                    encryptedResponse,
                    TransactionSuccessResponse.class
            );

            log.info("NTT Data response:\n{}",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(decrypted));

            String responseJson = objectMapper.writeValueAsString(decrypted);
            nttTxn.setResponsePayload(responseJson);
            nttTransactionRepository.save(nttTxn);

            masterTxn.setStatus(TransactionStatus.PENDING);
            masterTransactionRepository.save(masterTxn);

            log.info("Payment initiated successfully. Master ID: {}, NTT ID: {}",
                    masterTxn.getId(), nttTxn.getId());

            return decrypted;

        } catch (Exception e) {
            throw new RuntimeException("Payment initiation failed: " + e.getMessage(), e);
        }
    }
}