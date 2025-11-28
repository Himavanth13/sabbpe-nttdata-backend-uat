package com.sabbpe.nttdata.dtos;

import lombok.Data;
@Data
public class TransactionStatusRequest {

    private PayInstrument payInstrument;

    @Data
    public static class PayInstrument {
        private HeadDetails headDetails;
        private MerchDetails merchDetails;
        private PayDetails payDetails;
    }

    // ------------------ HEAD DETAILS ------------------
    @Data
    public static class HeadDetails {
        private String api;     // "TXNVERIFICATION"
        private String source;  // "OTS"
    }

    // ------------------ MERCHANT DETAILS ------------------
    @Data
    public static class MerchDetails {
        private Long merchId;       // 446442
        private String password;   // Test@123
        private String merchTxnId; // "67a44ce2ed4c6"
        private String merchTxnDate; // "2025-02-06"
    }

    // ------------------ PAYMENT DETAILS ------------------
    @Data
    public static class PayDetails {
        private String atomTxnId;   // "11000000631738" (STRING as per your JSON)
        private Double amount;     // 10.00
        private String txnCurrency; // "INR"
        private String signature;  // HMAC SHA-512 signature
    }
}
