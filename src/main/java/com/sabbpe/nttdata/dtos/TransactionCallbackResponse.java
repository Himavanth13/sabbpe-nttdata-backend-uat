package com.sabbpe.nttdata.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionCallbackResponse {

    private PayInstrument payInstrument;

    // ===================== PAY INSTRUMENT =====================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayInstrument {
        private MerchDetails merchDetails;
        private PayDetails payDetails;
        private PayModeSpecificData payModeSpecificData;
        private CustDetails custDetails;
        private ResponseDetails responseDetails;
        private Extras extras;
    }

    // ===================== MERCHANT DETAILS =====================
    @Data
    public static class MerchDetails {
        private Long merchId;
        private String merchTxnId;
        private String merchTxnDate;   // Matches JSON
    }

    // ===================== PAYMENT DETAILS =====================
    @Data
    public static class PayDetails {
        private Long atomTxnId;
        private List<ProductDetails> prodDetails;
        private Double amount;
        private Double surchargeAmount;
        private Double totalAmount;
        private String custAccNo;
        private String clientCode;
        private String txnInitDate;
        private String txnCompleteDate;
        private String txnCurrency;
        private String signature;
    }

    // ===================== PRODUCT DETAILS =====================
    @Data
    public static class ProductDetails {
        private String prodName;
        private Double prodAmount;
    }

    // ===================== CUSTOMER DETAILS =====================
    @Data
    public static class CustDetails {
        private String custFirstName;
        private String custEmail;
        private String custMobile;
        private BillingInfo billingInfo;
    }

    // ===================== BILLING INFO =====================
    @Data
    public static class BillingInfo {
        private String custAddr1;
        private String custAddr2;
        private String custCountry;
        private String custCity;
        private String custState;
        private String custZipCode;
    }

    // ===================== PAY MODE SPECIFIC DATA =====================
    @Data
    public static class PayModeSpecificData {
        private List<String> subChannel;
        private BankDetails bankDetails;
    }

    // ===================== BANK DETAILS =====================
    @Data
    public static class BankDetails {
        private Long otsBankId;           // FIXED → your JSON has a number
        private String bankTxnId;
        private String authId;
        private String otsBankName;
        private String cardType;
        private String cardMaskNumber;
        private String scheme;
    }

    // ===================== RESPONSE DETAILS =====================
    @Data
    public static class ResponseDetails {
        private String statusCode;
        private String message;
        private String description;
    }

    // ===================== EXTRAS / UDF =====================
    @Data
    public static class Extras {
        private String udf1;
        private String udf2;
        private String udf3;
        private String udf4;
        private String udf5;
        private String udf6;
        private String udf7;
        private String udf8;
        private String udf9;
        private String udf10;
    }
}
