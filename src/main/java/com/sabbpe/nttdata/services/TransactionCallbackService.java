package com.sabbpe.nttdata.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.nttdata.dtos.TransactionCallbackResponse;
import com.sabbpe.nttdata.models.PaymentsTransaction;
import com.sabbpe.nttdata.repositories.PaymentsTransactionRepository;
import com.sabbpe.nttdata.utils.NttCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionCallbackService {

    private final NttCrypto nttCrypto;
    private final PaymentsTransactionRepository paymentsTransactionRepository;


    private String FRONTEND_URL;

    @Transactional
    public String callback(String encData) {

        try {
            log.info("NDPS CALLBACK RECEIVED >> {}", encData);

            // 🔓 1. Decrypt callback JSON
            String decryptedJson = nttCrypto.decryptResponse(encData);

            log.info("Decrypted callback JSON: {}", decryptedJson);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(decryptedJson);

            // 🔍 2. Extract udf6 (token)
            String udf6 = root.path("payInstrument")
                    .path("extras")
                    .path("udf6")
                    .asText();

            // extract frontend url;
            FRONTEND_URL=root.path("payInstrument")
                    .path("extras")
                    .path("udf7")
                    .asText();

            log.info("Token extracted from callback (udf6): {}", udf6);

            // 🔍 3. Find PaymentsTransaction record by token
            PaymentsTransaction txn = paymentsTransactionRepository
                    .findTopByTransactionTokenOrderByCreatedAtDesc(udf6)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid / unknown token: " + udf6));

            // 📝 4. Parse callback JSON into DTO
            TransactionCallbackResponse callback =
                    mapper.readValue(decryptedJson, TransactionCallbackResponse.class);

            log.info("Mapped callback DTO:\n{}",
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(callback));

            // Extract fields
            String statusCode = callback.getPayInstrument().getResponseDetails().getStatusCode();

            String json= mapper.writerWithDefaultPrettyPrinter().writeValueAsString(callback);
            // 📝 5. UPDATE PaymentsTransaction table
            txn.setResponseMetadata(json);
            txn.setStatusCode(statusCode);


            // Optional: mark success/failed
            if ("OTS0000".equalsIgnoreCase(statusCode)) {
                txn.setStatusMessage("SUCCESS");
            } else {
                txn.setStatusMessage("FAILED");
            }

            paymentsTransactionRepository.save(txn);

            log.info("PaymentsTransaction updated successfully for token = {}", udf6);

            JsonNode merchTxnIdNode = root.path("payInstrument")
                    .path("merchDetails")
                    .path("merchTxnId");

            String merchTxnId = merchTxnIdNode.isMissingNode() ? "" : merchTxnIdNode.asText();
            String encodedTxnId = URLEncoder.encode(merchTxnId, StandardCharsets.UTF_8);

            String redirect = "redirect:" + FRONTEND_URL + "?txnId=" + encodedTxnId;

            log.info("redirect url : {}",redirect);
            return redirect;

        } catch (Exception e) {
            log.error("Callback processing FAILED", e);
            String redirect= "redirect:" + FRONTEND_URL + "?error=callback_failed";
            return redirect;
        }
    }
}
