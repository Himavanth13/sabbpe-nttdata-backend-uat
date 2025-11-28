package com.sabbpe.nttdata.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.nttdata.dtos.TransactionCallbackResponse;
import com.sabbpe.nttdata.utils.NttCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class TrasactionCallbackController {

    private final NttCrypto nttCrypto;
    @PostMapping("/initiatepaymentcallback")
    public ResponseEntity<String> handleCallback(@RequestParam("encData") String encData) {
        try {
            log.info(" CALLBACK RECEIVED");
            log.info("enc callback data : {}", encData);

            String decryptResponse = nttCrypto.decryptResponse(encData);
            log.info("decrypted data : {}", decryptResponse);

            ObjectMapper objectMapper = new ObjectMapper();

            // FIX #1 — Correct JSON Parsing
            TransactionCallbackResponse response =
                    objectMapper.readValue(decryptResponse, TransactionCallbackResponse.class);

            // Extract Required Fields
            TransactionCallbackResponse.PayInstrument payInstrument =
                    response.getPayInstrument();

            String merchId = String.valueOf(
                    payInstrument.getMerchDetails().getMerchId());

            String atomTxnId = String.valueOf(
                    payInstrument.getPayDetails().getAtomTxnId());

            String merchTxnId =
                    payInstrument.getMerchDetails().getMerchTxnId();

            String totalAmount = String.valueOf(
                    payInstrument.getPayDetails().getTotalAmount());

            String txnStatusCode =
                    payInstrument.getResponseDetails().getStatusCode();

            //  Safe SubChannel
            String subChannel = "";
            if (payInstrument.getPayModeSpecificData() != null
                    && payInstrument.getPayModeSpecificData().getSubChannel() != null
                    && !payInstrument.getPayModeSpecificData().getSubChannel().isEmpty()) {
                subChannel = payInstrument.getPayModeSpecificData()
                        .getSubChannel().get(0);
            }

            // Safe BankTxnId
            String bankTxnId = "";
            if (payInstrument.getPayModeSpecificData() != null
                    && payInstrument.getPayModeSpecificData().getBankDetails() != null) {
                bankTxnId = payInstrument.getPayModeSpecificData()
                        .getBankDetails()
                        .getBankTxnId();
            }
//            merchId + atomTxnId + merchTxnId + totalAmount + txnStatusCode + subChannel + bankTxnId

            // Raw Signature String (Exact Order)
            String raw =merchId +atomTxnId +merchTxnId +totalAmount +txnStatusCode +subChannel +bankTxnId;

            log.info(" RAW SIGN STRING: {}", raw);

            //  Generate Signature
            String generatedResponseSignature =
                    nttCrypto.generateResponseSignature(raw);
            generatedResponseSignature=generatedResponseSignature.toLowerCase();
            String responseSignature =
                    payInstrument.getPayDetails().getSignature();

            log.info("generatedResponseSignature : {}",generatedResponseSignature);
            log.info("responseSignature : {}",responseSignature);

            // ✅ Signature Validation
            if (!generatedResponseSignature.equals(responseSignature)) {
                log.error("❌ Callback signature INVALID");
                return ResponseEntity.badRequest().body("INVALID SIGNATURE");
            }

            log.info(" Callback signature VALID");
            return ResponseEntity.ok("SUCCESS");

        } catch (Exception e) {
            log.error(" CALLBACK PROCESSING FAILED", e);
            return ResponseEntity.internalServerError().body("FAILED");
        }
    }

}
