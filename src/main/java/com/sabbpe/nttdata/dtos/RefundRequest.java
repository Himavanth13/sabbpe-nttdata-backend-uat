package com.sabbpe.nttdata.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;


@Data
public class RefundRequest {

    @JsonProperty("payInstrument")
    private PayInstrument payInstrument;

    // ================== PAY INSTRUMENT ==================

    @Data
    public static class PayInstrument {

        @JsonProperty("headDetails")
        private HeadDetails headDetails;

        @JsonProperty("merchDetails")
        private MerchDetails merchDetails;

        @JsonProperty("payDetails")
        private PayDetails payDetails;
    }

    // ================== HEAD DETAILS ==================

    @Data
    public static class HeadDetails {

        @JsonProperty("api")
        private String api;

        @JsonProperty("source")
        private String source;
    }

    // ================== MERCHANT DETAILS ==================

    @Data
    public static class MerchDetails {

        @JsonProperty("merchId")
        private Integer merchId;

        @JsonProperty("password")
        private String password;

        @JsonProperty("merchTxnId")
        private String merchTxnId;
    }

    // ================== PAY DETAILS ==================

    @Data
    public static class PayDetails {

        @JsonProperty("signature")
        private String signature;

        @JsonProperty("atomTxnId")
        private Long atomTxnId;

        @JsonProperty("totalRefundAmount")
        private Double totalRefundAmount;

        @JsonProperty("txnCurrency")
        private String txnCurrency;

        @JsonProperty("prodDetails")
        private List<ProdDetails> prodDetails;
    }

    // ================== PRODUCT DETAILS ==================

    @Data
    public static class ProdDetails {

        @JsonProperty("prodName")
        private String prodName;

        @JsonProperty("prodRefundId")
        private String prodRefundId;

        @JsonProperty("prodRefundAmount")
        private Double prodRefundAmount;   // ✅ FIXED (was Integer)
    }
}
