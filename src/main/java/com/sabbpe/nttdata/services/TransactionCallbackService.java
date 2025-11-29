package com.sabbpe.nttdata.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.nttdata.dtos.TransactionCallbackResponse;
import com.sabbpe.nttdata.enums.TransactionStatus;
import com.sabbpe.nttdata.models.Transaction;
import com.sabbpe.nttdata.repositories.TransactionRepository;
import com.sabbpe.nttdata.utils.NttCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionCallbackService {

    private final NttCrypto nttCrypto;
    private final TransactionRepository txnRepository;

    @Value("${ndps.trxcallbackurlpage}")
    private String FRONTEND_URL;

    @Transactional
    public String callback(String encData) {
        try {
            log.info("NDPS CALLBACK RECEIVED");
            log.info("Encrypted callback data: {}", encData);

            // 🔓 Decrypt the callback payload
            String decryptedJson = nttCrypto.decryptResponse(encData);
            log.info("Decrypted callback JSON: {}", decryptedJson);

            ObjectMapper objectMapper = new ObjectMapper();

            // ✅ Parse JSON to DTO
            TransactionCallbackResponse response =
                    objectMapper.readValue(decryptedJson, TransactionCallbackResponse.class);

            String transactionId =response.getPayInstrument().getExtras().getUdf1();

            TransactionCallbackResponse.PayInstrument payInstrument = response.getPayInstrument();

            // ✅ Extract core fields
            String merchId = String.valueOf(payInstrument.getMerchDetails().getMerchId());
            String atomTxnId = String.valueOf(payInstrument.getPayDetails().getAtomTxnId());
            String merchTxnId = payInstrument.getMerchDetails().getMerchTxnId();
            String totalAmount = String.valueOf(payInstrument.getPayDetails().getTotalAmount());
            String txnStatusCode = payInstrument.getResponseDetails().getStatusCode();

            // ✅ Safe subChannel
            String subChannel = "";
            if (payInstrument.getPayModeSpecificData() != null
                    && payInstrument.getPayModeSpecificData().getSubChannel() != null
                    && !payInstrument.getPayModeSpecificData().getSubChannel().isEmpty()) {
                subChannel = payInstrument.getPayModeSpecificData()
                        .getSubChannel()
                        .get(0);
            }

            // ✅ Safe bankTxnId
            String bankTxnId = "";
            if (payInstrument.getPayModeSpecificData() != null
                    && payInstrument.getPayModeSpecificData().getBankDetails() != null) {
                bankTxnId = payInstrument.getPayModeSpecificData()
                        .getBankDetails()
                        .getBankTxnId();
            }

            // 🔑 Raw signature string in **exact** order
            String raw = merchId
                    + atomTxnId
                    + merchTxnId
                    + totalAmount
                    + txnStatusCode
                    + subChannel
                    + bankTxnId;

            log.info("RAW SIGN STRING: {}", raw);

            // 🔐 Generate signature
            String generatedResponseSignature = nttCrypto.generateResponseSignature(raw);
            generatedResponseSignature = generatedResponseSignature.toLowerCase();

            String responseSignature = payInstrument.getPayDetails().getSignature();
            if (responseSignature != null) {
                responseSignature = responseSignature.toLowerCase();
            }

            log.info("Generated response signature : {}", generatedResponseSignature);
            log.info("Callback response signature  : {}", responseSignature);

            // ✅ Signature Validation
            if (responseSignature == null || !generatedResponseSignature.equals(responseSignature)) {
                log.error("Callback signature INVALID for atomTxnId={}", atomTxnId);
                return "redirect:" + FRONTEND_URL + "/payment-result?error=invalid_signature";
            }

            log.info("Callback signature VALID for atomTxnId={}", atomTxnId);

            // ✅ Lookup the transaction using authCode (atomTxnId)
            Optional<Transaction> optionalTxn = txnRepository.findByTransactionIdAndMerchantOrderId(transactionId,merchTxnId);

            if (optionalTxn.isEmpty()) {
                log.error("Transaction not found for atomTxnId={} (merchTxnId={})", atomTxnId, merchTxnId);
                return "redirect:" + FRONTEND_URL + "/payment-result?error=txn_not_found";
            }
            Transaction txn = optionalTxn.get();

            if (txn.getStatus() == TransactionStatus.SUCCESS) {
                log.warn("Duplicate callback ignored for txnId={} merchTxnId={} atomTxnId={}",
                        txn.getTransactionId(), merchTxnId, atomTxnId);
                return "redirect:" + FRONTEND_URL + "/payment-result?txnId=" + merchTxnId;
            }

            txn.setResponseMetadata(decryptedJson);

            if ("OTS0000".equalsIgnoreCase(txnStatusCode)) {
                txn.setStatus(TransactionStatus.SUCCESS);
            } else {
                txn.setStatus(TransactionStatus.FAILED);
            }

            txnRepository.saveAndFlush(txn);

            log.info("Callback processed and transaction updated. txnId={}, merchTxnId={}, status={}",
                    txn.getTransactionId(), merchTxnId, txn.getStatus());

            // ✅ Redirect to frontend success page
            return "redirect:" + FRONTEND_URL + "/payment-result?txnId=" + merchTxnId;

        } catch (Exception e) {
            log.error("CALLBACK PROCESSING FAILED", e);
            return "redirect:" + FRONTEND_URL + "/payment-result?error=callback_processing_failed";
        }
    }
}
