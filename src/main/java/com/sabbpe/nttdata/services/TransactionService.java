package com.sabbpe.nttdata.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.nttdata.dtos.TransactionRequest;
import com.sabbpe.nttdata.dtos.TransactionSuccessResponse;
import com.sabbpe.nttdata.models.PaymentsTransaction;
import com.sabbpe.nttdata.repositories.PaymentsTransactionRepository;
import com.sabbpe.nttdata.utils.AESUtil;
import com.sabbpe.nttdata.utils.NttCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final RestTemplate restTemplate;
    private final NttCrypto nttCrypto;
    private final PaymentsTransactionRepository paymentsTransactionRepository;
    private final ClientProfileService clientProfileService;

    @Value("${ndps.auth-url}")
    private String authUrl;

    @Value("${ndps.merch-id}")
    private String merchId;
    @Value("${ndps.password}")
    private String password;

    private static final DateTimeFormatter TS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    // ==========================================================
    // VALIDATE TOKEN (FIXED)
    // ==========================================================
    private void validateTransactionToken(String token) throws Exception {

        PaymentsTransaction tokenTxn = paymentsTransactionRepository
                .findTopByTransactionTokenOrderByCreatedAtDesc(token)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid or unknown transaction token"));

        String clientId = tokenTxn.getClientId();

        Map<String, Object> crypto = clientProfileService.getCryptoByClientId(clientId);

        if (crypto == null || crypto.isEmpty()) {
            throw new IllegalStateException("No crypto config found for client: " + clientId);
        }

        String transactionUserId   = String.valueOf(crypto.get("transactionUserId"));
        String transactionPassword = String.valueOf(crypto.get("transactionPassword"));
        String aesKey              = String.valueOf(crypto.get("transactionAesKey"));
        String aesIv               = String.valueOf(crypto.get("transactionIv"));
        String transactionMerchantId = String.valueOf(crypto.get("transactionMerchantId"));

        LocalDateTime ts = tokenTxn.getMerchantTransactionTimestamp();
        if (ts == null) {
            throw new IllegalStateException("Missing merchant transaction timestamp");
        }

        String expectedRaw = transactionUserId
                + transactionMerchantId
                + transactionPassword
                + ts.format(TS_FORMATTER);

        String decryptedRaw = AESUtil.decrypt(token, aesKey, aesIv);

        if (!expectedRaw.equals(decryptedRaw)) {
            throw new IllegalArgumentException("Invalid transaction token (payload mismatch)");
        }

        log.info("Transaction token validated for client_id={} merchantId={}",
                clientId, transactionMerchantId);
    }


    // ==========================================================
    // INITIATE PAYMENT (FULL UPDATE USING TOKEN)
    // ==========================================================
    public TransactionSuccessResponse initiate(TransactionRequest request)
            throws JsonProcessingException {

        log.info("Incoming initiation request:\n{}",
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(request));

        try {
            // ---------------------------
            // Extract customer details
            // ---------------------------
            String custEmail = request.getPayInstrument().getCustDetails().getCustEmail();
            String custMobile = request.getPayInstrument().getCustDetails().getCustMobile();

            if (custEmail == null || custEmail.isBlank()
                    || custMobile == null || custMobile.isBlank()) {
                throw new IllegalArgumentException("Customer email or mobile missing");
            }

            // Fetch mapping
            Map<String, Object> nttMapping =
                    clientProfileService.getNttMappingByCustomer(custEmail, custMobile);

            if (nttMapping == null || nttMapping.isEmpty()) {
                throw new IllegalArgumentException("Customer not mapped to client_profile");
            }

            String clientIdFromProfile = String.valueOf(nttMapping.get("clientId"));
            String nttUserId           = String.valueOf(nttMapping.get("nttUserId"));
            String nttPassword         = String.valueOf(nttMapping.get("nttPassword"));
            String nttMerchantId       = String.valueOf(nttMapping.get("nttMerchantId"));

            // Replace NDPS credentials
            request.getPayInstrument().getMerchDetails().setUserId(merchId);
            request.getPayInstrument().getMerchDetails().setMerchId(merchId);
            request.getPayInstrument().getMerchDetails().setPassword(password);
            ObjectMapper objectMapper=new ObjectMapper();
            String payload=objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            log.info("request metadata after setting credentials : {}",payload);
            // ---------------------------
            // Validate transaction token
            // ---------------------------
            String token = request.getPayInstrument().getExtras().getUdf3();
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("Missing transaction token in udf3");
            }

            validateTransactionToken(token);

            // ---------------------------
            // Fetch existing txn row (this ensures update)
            // ---------------------------
            PaymentsTransaction txn = paymentsTransactionRepository
                    .findTopByTransactionTokenOrderByCreatedAtDesc(token)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

            // ---------------------------
            // UPDATE REQUEST METADATA
            // ---------------------------
            ObjectMapper mapper = new ObjectMapper();
            String reqJson = mapper.writeValueAsString(request);

            txn.setRequestMetadata(reqJson);
            txn.setCustEmail(custEmail);
            txn.setCustMobile(custMobile);
            txn.setClientId(clientIdFromProfile);
            txn.setUdf1(txn.getId()); // Saving UUID in udf1 (correct)

            paymentsTransactionRepository.save(txn);   // <-- THIS IS UPDATE


            // ---------------------------
            // Encrypt and call NDPS
            // ---------------------------
            String encData = nttCrypto.encryptRequest(request);

            String form = "encData=" + URLEncoder.encode(encData, StandardCharsets.UTF_8)
                    + "&merchId=" + merchId;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<String> response = restTemplate.exchange(
                    authUrl, HttpMethod.POST, new HttpEntity<>(form, headers), String.class);

            if (response.getBody() == null || !response.getBody().contains("encData=")) {
                log.info("respose from ndps : {}",response.getBody());
                return null;
            }

            String encryptedResponse =
                    response.getBody().substring(response.getBody().indexOf("encData=") + 8);

            TransactionSuccessResponse decrypted =
                    nttCrypto.decryptResponse(encryptedResponse, TransactionSuccessResponse.class);

            // ---------------------------
            // UPDATE RESPONSE METADATA
            // ---------------------------
            txn.setResponseMetadata(mapper.writeValueAsString(decrypted));
            paymentsTransactionRepository.save(txn);   // <-- UPDATE AGAIN

            return decrypted;

        } catch (Exception e) {
            throw new RuntimeException("Payment initiation failed: " + e.getMessage(), e);
        }
    }
}
