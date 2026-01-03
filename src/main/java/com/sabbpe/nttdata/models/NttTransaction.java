package com.sabbpe.nttdata.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "nttdata_processor_transaction_details")
public class NttTransaction {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "master_transaction_id", length = 36, nullable = false)
    private String masterTransactionId;

    @Column(name = "merch_txn_id", length = 128)
    private String merchTxnId;

    @Column(name = "atom_token_id")
    private Long atomTokenId;

    @Column(name = "atom_txn_id")
    private Long atomTxnId;

    @Column(name = "merchant_user_id", length = 64)
    private String merchantUserId;

    @Column(name = "merchant_id", length = 64)
    private String merchantId;

    @Column(name = "pg_merchant_id", length = 255)
    private String pgMerchantId;

    @Column(name = "api", length = 32)
    private String api;

    @Column(name = "api_version", length = 32)
    private String apiVersion;

    @Column(name = "platform", length = 32)
    private String platform;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "surcharge_amount", precision = 12, scale = 2)
    private BigDecimal surchargeAmount;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "customer_first_name", length = 255)
    private String customerFirstName;

    @Column(name = "customer_last_name", length = 255)
    private String customerLastName;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_mobile", length = 32)
    private String customerMobile;

    @Column(name = "customer_account_no", length = 64)
    private String customerAccountNo;

    @Column(name = "customer_ifsc", length = 32)
    private String customerIfsc;

    @Column(name = "customer_addr1", length = 255)
    private String customerAddr1;

    @Column(name = "customer_addr2", length = 255)
    private String customerAddr2;

    @Column(name = "customer_city", length = 64)
    private String customerCity;

    @Column(name = "customer_state", length = 64)
    private String customerState;

    @Column(name = "customer_country", length = 64)
    private String customerCountry;

    @Column(name = "customer_zip", length = 32)
    private String customerZip;

    @Column(name = "txn_init_date")
    private LocalDateTime txnInitDate;

    @Column(name = "txn_complete_date")
    private LocalDateTime txnCompleteDate;

    @Column(name = "sub_channel", length = 32)
    private String subChannel;

    @Column(name = "bank_id")
    private Integer bankId;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "bank_txn_id", length = 64)
    private String bankTxnId;

    @Column(name = "auth_id", length = 32)
    private String authId;

    @Column(name = "card_type", length = 32)
    private String cardType;

    @Column(name = "card_masked_number", length = 32)
    private String cardMaskedNumber;

    @Column(name = "card_scheme", length = 32)
    private String cardScheme;

    @Column(name = "mcc_code", length = 32)
    private String mccCode;

    @Column(name = "gateway_status_code", length = 32)
    private String gatewayStatusCode;

    @Column(name = "gateway_status_message", length = 255)
    private String gatewayStatusMessage;

    @Column(name = "gateway_status_description", length = 255)
    private String gatewayStatusDescription;

    @Column(name = "udf1", length = 255)
    private String udf1;

    @Column(name = "udf2", length = 255)
    private String udf2;

    @Column(name = "udf3", length = 255)
    private String udf3;

    @Column(name = "udf4", length = 255)
    private String udf4;

    @Column(name = "udf5", length = 255)
    private String udf5;

    @Column(name = "udf6", length = 255)
    private String udf6;

    @Column(name = "udf7", length = 255)
    private String udf7;

    @Column(name = "udf8", length = 255)
    private String udf8;

    @Column(name = "udf9", length = 255)
    private String udf9;

    @Column(name = "udf10", length = 255)
    private String udf10;

    @Column(name = "signature", columnDefinition = "TEXT")
    private String signature;

    @Column(name = "request_payload", columnDefinition = "LONGTEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "LONGTEXT")
    private String responsePayload;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
