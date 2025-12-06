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
import com.sabbpe.nttdata.services.ClientProfileService;
import com.sabbpe.nttdata.utils.AESUtil;
import com.sabbpe.nttdata.dtos.TransactionRequest.MerchDetails;

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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final RestTemplate restTemplate;
    private final NttCrypto nttCrypto; // inject crypto class
    private final TransactionRepository transactionRepository;

    private final ClientProfileService clientProfileService;

    @Value("${ndps.auth-url}")
    private String authUrl;

    @Value("${ndps.merch-id}")
    private String merchId;

    private static final DateTimeFormatter TS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private void validateTransactionToken(String token) throws Exception {

        // 1) Find the transaction row that was created in /generate/transactiontoken
        Transaction tokenTxn = transactionRepository
                .findTopByTransactionTokenOrderByCreatedAtDesc(token)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid or unknown transaction token"));

        // 2) Load crypto & credentials from client_profile using client_id
        String clientId = tokenTxn.getClientId();

        Map<String, Object> crypto =
                clientProfileService.getCryptoByClientId(clientId);

        if (crypto == null || crypto.isEmpty()) {
            throw new IllegalStateException("No crypto config found for client: " + clientId);
        }

        String transactionUserId   = String.valueOf(crypto.get("transactionUserId"));
        String transactionPassword = String.valueOf(crypto.get("transactionPassword"));
        String aesKey              = String.valueOf(crypto.get("transactionAesKey"));
        String aesIv               = String.valueOf(crypto.get("transactionIv"));

        // 3) Rebuild the same raw string that was encrypted in /generate/transactiontoken
        //    raw = transactionUserId + transactionMerchantId + password + timestamp
        String transactionMerchantId = tokenTxn.getMerchantOrderId(); // you set this when generating token

        LocalDateTime ts = tokenTxn.getMerchantTransactionTimestamp();
        if (ts == null) {
            throw new IllegalStateException("Missing merchant transaction timestamp for token transaction");
        }
        String normalizedTs = ts.format(TS_FORMATTER);

        String expectedRaw = transactionUserId
                + transactionMerchantId
                + transactionPassword
                + normalizedTs;

        // 4) Decrypt the token using the same AES key/IV
        String decryptedRaw = AESUtil.decrypt(token, aesKey, aesIv);

        // 5) Compare decrypted value with expected raw string
        if (!expectedRaw.equals(decryptedRaw)) {
            throw new IllegalArgumentException("Invalid transaction token (payload mismatch)");
        }

        log.info("Transaction token validated successfully for client_id={}, merchantId={}",
                clientId, transactionMerchantId);
    }

    public TransactionSuccessResponse initiate(TransactionRequest request) {

        try {
            // 🔹 0a) Extract customer details from request JSON
            String custEmail = request.getPayInstrument()
                    .getCustDetails()
                    .getCustEmail();

            String custMobile = request.getPayInstrument()
                    .getCustDetails()
                    .getCustMobile();

            if (custEmail == null || custEmail.isBlank()
                    || custMobile == null || custMobile.isBlank()) {
                throw new IllegalArgumentException("Customer email/mobile is required");
            }

            // 🔹 0b) Validate custEmail & custMobile against client_profile
            Map<String, Object> nttMapping =
                    clientProfileService.getNttMappingByCustomer(custEmail, custMobile);

            if (nttMapping == null || nttMapping.isEmpty()) {
                throw new IllegalArgumentException(
                        "No client_profile found for given customer email/mobile");
            }

            String clientIdFromProfile = String.valueOf(nttMapping.get("clientId"));
            String nttUserId           = String.valueOf(nttMapping.get("nttUserId"));
            String nttPassword         = String.valueOf(nttMapping.get("nttPassword"));
            String nttMerchantId       = String.valueOf(nttMapping.get("nttMerchantId"));

            if (nttUserId == null || nttUserId.equals("null")
                    || nttPassword == null || nttPassword.equals("null")
                    || nttMerchantId == null || nttMerchantId.equals("null")) {
                throw new IllegalStateException(
                        "NTTDATA credentials (ntt_userid/ntt_password/ntt_merchantid) " +
                                "are not configured for this customer");
            }

            // 🔹 0c) Replace NDPS/NTTDATA credentials in request JSON
            // 🔹 0c) Replace NDPS/NTTDATA credentials in request JSON
            MerchDetails merchDetails = request.getPayInstrument().getMerchDetails();

// (Keep merchTxnId / merchTxnDate from incoming request)
            merchDetails.setUserId(nttUserId);        // 3) replace userId
            merchDetails.setMerchId(nttMerchantId);   // 4) replace merchId
            merchDetails.setPassword(nttPassword);    // 5) replace password

            // 0d) Validate token in Extras.udf3
            String token = request.getPayInstrument()
                    .getExtras()
                    .getUdf3();

            if (token == null || token.isBlank() || "-".equals(token)) {
                throw new IllegalArgumentException("Missing transaction token in Extras.udf3");
            }

            // This will throw if invalid
            validateTransactionToken(token);

            // 1) Create a new transaction record for this NDPS initiation
            Transaction txn = new Transaction();

            // ✅ Use client_id from client_profile
            txn.setClientId(clientIdFromProfile);

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

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonreq = objectMapper.writeValueAsString(request);
            txn.setRequestMetadata(jsonreq);
            transactionRepository.save(txn);
            log.info("Transaction Request payload: {}", request);

            String uuid = txn.getTransactionId();
            request.getPayInstrument().getExtras().setUdf1(uuid);

// ==========================================
// PRINT FULL FINAL JSON BEFORE ENCRYPTION
// ==========================================
            String prettyJsonPayload = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(request);

            log.info("📤 FINAL JSON PAYLOAD SENT TO NTTDATA:\n{}", prettyJsonPayload);

// 🔐 Step 1: Encrypt request JSON (with updated NTT credentials)
            String encData = nttCrypto.encryptRequest(request);


            log.info("encrypted data : {}", encData);
            String form = "encData=" + URLEncoder.encode(encData, StandardCharsets.UTF_8)
                    + "&merchId=" + merchId; // <- This is global NDPS merchId (config) – keep or change as per NDPS spec
            log.info("prepared url : {}", form);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> entity = new HttpEntity<>(form, headers);

            // 📡 Step 2: Call NTTDATA / NDPS AUTH API
            ResponseEntity<String> response =
                    restTemplate.exchange(authUrl, HttpMethod.POST, entity, String.class);

            Optional<Transaction> opttxn = transactionRepository.findByTransactionId(uuid);

            Transaction txn1 = null;
            if (opttxn.isPresent()) {
                txn1 = opttxn.get();
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

            // ✅ GET atomTokenId FROM RESPONSE
            Long atomTokenId = decrypted.getAtomTokenId();
            txn1.setAuthCode(String.valueOf(atomTokenId));
            log.info("Atom Token ID from NDPS: {}", atomTokenId);

            String obj = objectMapper.writeValueAsString(decrypted);

            ObjectNode objectNode = (ObjectNode) objectMapper.readTree(obj);

            objectNode.put(
                    "merchTxnId",
                    request.getPayInstrument().getMerchDetails().getMerchTxnId()
            );

            String finalJson = objectMapper.writeValueAsString(objectNode);

            txn1.setResponseMetadata(finalJson);

            transactionRepository.save(txn1);

            return decrypted;

        } catch (Exception e) {
            throw new RuntimeException("Payment initiation failed: " + e.getMessage(), e);
        }
    }
}