package com.sabbpe.nttdata.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sabbpe.nttdata.dtos.TransactionRequest;
import com.sabbpe.nttdata.dtos.TransactionSuccessResponse;

import com.sabbpe.nttdata.enums.PaymentProvider;
import com.sabbpe.nttdata.enums.PaymentMethod;
import com.sabbpe.nttdata.enums.TransactionStatus;
import com.sabbpe.nttdata.models.Transaction;
import com.sabbpe.nttdata.repositories.TransactionRepository;
import com.sabbpe.nttdata.utils.NttCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final RestTemplate restTemplate;
    private final NttCrypto nttCrypto; // inject crypto class
    private final TransactionRepository transactionRepository;
    @Value("${ndps.auth-url}")
    private String authUrl;

    @Value("${ndps.merchId}")
    private String merchId;

    public TransactionSuccessResponse initiate(TransactionRequest request) {

        try {

            Transaction txn = new Transaction();

// ✅ Store FULL REQUEST JSON


            txn.setClientId(request.getPayInstrument()
                    .getMerchDetails()
                    .getMerchId());

            txn.setMerchantOrderId(request.getPayInstrument()
                    .getMerchDetails()
                    .getMerchTxnId());

            Double reqAmount = request.getPayInstrument()
                    .getPayDetails()
                    .getAmount();
            txn.setAmount(BigDecimal.valueOf(reqAmount));

            txn.setCurrency(request.getPayInstrument()
                    .getPayDetails()
                    .getTxnCurrency());

            String subChannel = request.getPayInstrument()
                    .getPayModeSpecificData()
                    .getSubChannel();

            if ("DC".equalsIgnoreCase(subChannel)) {
                txn.setPaymentMethod(PaymentMethod.DEBIT_CARD);
            } else if ("CC".equalsIgnoreCase(subChannel)) {
                txn.setPaymentMethod(PaymentMethod.CREDIT_CARD);
            } else {
                txn.setPaymentMethod(PaymentMethod.UPI);
            }

            txn.setPaymentProvider(PaymentProvider.NDPS);
            txn.setStatus(TransactionStatus.INITIATED);
//            request.getPayInstrument().getExtras().setUdf2(request.getPayInstrument().getMerchDetails().getMerchTxnId());
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonreq = objectMapper.writeValueAsString(request);


            txn.setRequestMetadata(jsonreq);
            transactionRepository.save(txn);


            log.info("Transaction Request payload: {}",request);
            String uuid=txn.getTransactionId();

            request.getPayInstrument().getExtras().setUdf1(uuid);
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


            Optional<Transaction> opttxn=transactionRepository.findByTransactionId(uuid);

            Transaction txn1=null;
            if(opttxn.isPresent()) {
                txn1=opttxn.get();
            }

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


// ✅ ✅ GET atomTokenId FROM *RESPONSE*, NOT REQUEST
            Long atomTokenId = decrypted.getAtomTokenId();
            txn1.setAuthCode(String.valueOf(atomTokenId));
            log.info("Atom Token ID from NDPS: {}", atomTokenId);

            String obj = objectMapper.writeValueAsString(decrypted);

// ✅ Convert JSON String → ObjectNode properly
            ObjectNode objectNode = (ObjectNode) objectMapper.readTree(obj);

// ✅ Now safely add your new field
            objectNode.put(
                    "merchantTxnId",
                    request.getPayInstrument().getMerchDetails().getMerchTxnId()
            );

// ✅ Convert back to JSON if needed
            String finalJson = objectMapper.writeValueAsString(objectNode);



// ✅ OPTIONAL: store it in response_metadata
            txn1.setResponseMetadata(
                    objectMapper.writeValueAsString(finalJson)
            );

// ✅ Save transaction
            transactionRepository.save(txn1);

            return decrypted;

        } catch (Exception e) {
            throw new RuntimeException("Payment initiation failed: " + e.getMessage(), e);
        }
    }
}