package com.sabbpe.nttdata.services;

import com.sabbpe.nttdata.dtos.TransactionRequest;
import com.sabbpe.nttdata.dtos.TransactionSuccessResponse;
import com.sabbpe.nttdata.utils.NttCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final RestTemplate restTemplate;
    private final NttCrypto nttCrypto; // inject crypto class

    @Value("${ndps.auth-url}")
    private String authUrl;

    @Value("${ndps.merchId}")
    private String merchId;

    public TransactionSuccessResponse initiate(TransactionRequest request) {

        try {
            log.info("Transaction Request payload: {}",request);
            // 🔐 Step 1: Encrypt request JSON
            String encData = nttCrypto.encryptRequest(request);

            log.info("encrypted data : {}",encData);
            String form = "encData=" + URLEncoder.encode(encData, StandardCharsets.UTF_8)
                    + "&merchId=" + merchId;
            log.info("prepared url : {}",form);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> entity = new HttpEntity<>(form, headers);

            // 📡 Step 2: Call NDPS AUTH API
            ResponseEntity<String> response =
                    restTemplate.exchange(authUrl, HttpMethod.POST, entity, String.class);

            String body = response.getBody();
            if (body == null || !body.contains("encData=")) {
                throw new RuntimeException("Invalid NDPS response: " + body);
            }

            // 🔍 Step 3: Extract encData from response
            String encryptedResponse = body.substring(body.indexOf("encData=") + 8);

            // 🔓 Step 4: Decrypt response JSON
            TransactionSuccessResponse decrypted =
                    nttCrypto.decryptResponse(encryptedResponse, TransactionSuccessResponse.class);

            // 🧾 Step 5: Check txnStatusCode
            String statusCode = decrypted.getResponseDetails().getTxnStatusCode();
            if (!"OTS0000".equals(statusCode)) {
                throw new RuntimeException("Initiation failed: " +
                        decrypted.getResponseDetails().getTxnDescription());
            }

            return decrypted;

        } catch (Exception e) {
            throw new RuntimeException("Payment initiation failed: " + e.getMessage(), e);
        }
    }
}