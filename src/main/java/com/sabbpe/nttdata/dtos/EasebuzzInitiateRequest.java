package com.sabbpe.nttdata.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EasebuzzInitiateRequest {

    // Optional: Override credentials (usually not sent)
    private String key;
    private String salt;

    // Required fields
    private String txnid;
    private String amount;
    private String productinfo;
    private String firstname;
    private String phone;
    private String email;
    private String surl;
    private String furl;

    // UDF fields
    private String udf1 = "";
    private String udf2 = "";
    private String udf3 = "";
    private String udf4 = "";
    private String udf5 = "";
    private String udf6 = "";
    private String udf7 = "";
    private String udf8 = "";
    private String udf9 = "";
    private String udf10 = "";


    // Address fields
    private String address1 = "";
    private String address2 = "";
    private String city = "";
    private String state = "";
    private String country = "";
    private String zipcode = "";

    // Optional fields
    @JsonProperty("show_payment_mode")
    private String showPaymentMode = "";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("split_payments")
    private Map<String, Object> splitPayments;

    @JsonProperty("request_flow")
    private String requestFlow = "";

    @JsonProperty("sub_merchant_id")
    private String subMerchantId = "";

    @JsonProperty("payment_category")
    private String paymentCategory = "";

    @JsonProperty("account_no")
    private String accountNo = "";

    private String ifsc = "";

    @JsonProperty("unique_id")
    private String uniqueId = "";
}
