package com.sabbpe.nttdata.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequest {

    private PayInstrument payInstrument = new PayInstrument();

    @Data
    public static class PayInstrument {
        private Extras Extras = new Extras();                         // Default "-"
        private PayDetails PayDetails = new PayDetails();             // Partially default
        private CustDetails CustDetails = new CustDetails();          // User input
        private HeadDetails HeadDetails = new HeadDetails();          // Always constant
        private MerchDetails MerchDetails = new MerchDetails();       // Partially default
        private PayModeSpecificData PayModeSpecificData = new PayModeSpecificData(); // Optional
    }

    @Data
    public static class Extras {
        private String udf1 = "-";
        private String udf2 = "-";
        private String udf3 = "-";
        private String udf4 = "-";
        private String udf5 = "-";
    }

    @Data
    public static class PayDetails {
        private Double amount;                             // MUST CHANGE EVERY TIME
        //        private String product = "NSE";
        private String product = "ONE78";
        private String custAccNo;                   // TPV only → null for normal merchants
        private String txnCurrency = "INR";                 // ALWAYS SAME
    }

    @Data
    public static class CustDetails {
        private String custEmail;                           // USER INPUT
        private String custMobile;                          // USER INPUT
    }

    @Data
    public static class HeadDetails {
        private String api = "AUTH";                        // ALWAYS SAME
        private String version = "OTSv1.1";                 // ALWAYS SAME
        private String platform = "FLASH";                  // ALWAYS SAME
    }

    @Data
    public static class MerchDetails {

        //        private String userId="446442";
        private String userId="770226";

        //        private String merchId="446442";
        private String merchId="770226";

        //        private String password="Test@123";            // ALWAYS SAME (replace with your NDPS password)
        private String password="770226_titan@123";            // ALWAYS SAME (replace with your NDPS password)
        private String merchTxnId= "TXN"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + ThreadLocalRandom.current().nextInt(1000, 9999);                        // MUST CHANGE EVERY TIME (unique)
        private String merchTxnDate;                        // MUST CHANGE EVERY TIME (yyyy-MM-dd HH:mm:ss)
    }

    @Data
    public static class PayModeSpecificData {
        private String subChannel = null;                   // OPTIONAL → null = enable all payment modes
    }
}