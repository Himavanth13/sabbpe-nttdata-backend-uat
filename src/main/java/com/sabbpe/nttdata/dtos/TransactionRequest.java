package com.sabbpe.nttdata.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequest {

    @JsonProperty("payInstrument")
    private PayInstrument payInstrument;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayInstrument {

        @JsonProperty("extras")
        private Extras extras;

        @JsonProperty("payDetails")
        private PayDetails payDetails;

        @JsonProperty("custDetails")
        private CustDetails custDetails;

        @JsonProperty("headDetails")
        private HeadDetails headDetails;

        @JsonProperty("merchDetails")
        private MerchDetails merchDetails;

        @JsonProperty("payModeSpecificData")
        private PayModeSpecificData payModeSpecificData;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Extras {
        private String udf1;
        private String udf2;
        private String udf3;
        private String udf4;
        private String udf5;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayDetails {
        private Double amount;
        private String product;
        private String custAccNo;
        private String txnCurrency;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustDetails {
        private String custEmail;
        private String custMobile;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HeadDetails {
        private String api;
        private String version;
        private String platform;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MerchDetails {
        private String userId;
        private String merchId;
        private String password;
        private String merchTxnId;
        private String merchTxnDate; // you'll parse this separately if needed
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayModeSpecificData {
        private String subChannel;
    }
}
