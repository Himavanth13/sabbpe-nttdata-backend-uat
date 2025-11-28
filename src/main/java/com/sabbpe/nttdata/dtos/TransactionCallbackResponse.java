package com.sabbpe.nttdata.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.List;
import com.sabbpe.nttdata.utils.NttCrypto;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionCallbackResponse {

    private PayInstrument payInstrument;
    private Extras extras;
    private CustDetails custDetails;
    private ResponseDetails responseDetails;

    // ===================== PAY INSTRUMENT =====================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayInstrument {
        private MerchDetails merchDetails;
        private PayDetails payDetails;
        private PayModeSpecificData payModeSpecificData;
    }

    // ===================== MERCHANT DETAILS =====================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MerchDetails {
        private Long merchId;
        private String merchTxnId;
        private String merchTxnDate;
    }

    // ===================== PAY DETAILS ✅ =====================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayDetails {

        private Long atomTxnId;

        private List<ProdDetails> prodDetails;

        private Double amount;
        private Double surchargeAmount;
        private Double totalAmount;

        private String custAccNo;
        private String clientCode;

        private String txnCurrency;   // ✅ REQUIRED

        private String signature;
        private String txnInitDate;
        private String txnCompleteDate;
    }

    // ===================== PRODUCT DETAILS =====================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProdDetails {
        private String prodName;
        private Double prodAmount;
    }

    // ===================== PAY MODE SPECIFIC DATA ✅ =====================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayModeSpecificData {

        // ✅ FIX: Array → List (This caused your current crash)
        private List<String> subChannel;

        private BankDetails bankDetails;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BankDetails {
        private Integer otsBankId;
        private String bankTxnId;
        private String authId;
        private String otsBankName;
        private String cardType;
        private String cardMaskNumber;
        private String scheme;
    }

    // ===================== EXTRAS =====================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Extras {
        private String udf1;
        private String udf2;
        private String udf3;
        private String udf4;
        private String udf5;
    }

    // ===================== CUSTOMER DETAILS =====================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustDetails {
        private String custEmail;
        private String custMobile;
        private BillingInfo billingInfo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BillingInfo {
        // Empty object in JSON → keep empty class
    }

    // ===================== RESPONSE DETAILS =====================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseDetails {
        private String statusCode;
        private String message;
        private String description;
    }









    /** 🔐 Validate callback signature */
//    public boolean verifySignature( NttCrypto crypto) {
//        PayInstrument r = this.getPayInstrument();
////        String raw = r.getMerchDetails().getMerchId()
////                + r.getPayDetails().getAtomTxnId()
////                + r.getMerchDetails().getMerchTxnId()
////                + String.format("%.2f", r.getPayDetails().getTotalAmount())
////                + r.getResponseDetails().getStatusCode()
////                + r.getPayModeSpecificData().getSubChannel()[0]
////                + r.getPayModeSpecificData().getBankDetails().getBankTxnId();
//
//
//        String merchId=String.valueOf(r.getMerchDetails().getMerchId());
//        String merchTxnId=r.getMerchDetails().merchTxnId;
//        double amount= r.getPayDetails().getAmount();
//        String currency="INR";
//
//        String calculated = crypto.generateStatusSignature( merchId, merchTxnId,  amount, currency);
//
//        return calculated.equalsIgnoreCase(r.getPayDetails().getSignature());
//
//    }

}
