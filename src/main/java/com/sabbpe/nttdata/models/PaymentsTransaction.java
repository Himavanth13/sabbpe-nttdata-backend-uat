package com.sabbpe.nttdata.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentsTransaction {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "merchTxnId", nullable = false, length = 128)
    private String merchantTransactionId;

    @Column(name = "merchant_transaction_timestamp")
    private LocalDateTime merchantTransactionTimestamp;

    @Column(name = "transaction_token", columnDefinition = "longtext")
    private String transactionToken;

    @Column(name = "userId")
    private String userId;

    @Column(name = "merchId")
    private String transactionMerchantId;

    @Column(name = "password")
    private String password;

    @Column(name = "merchTxnDate")
    private LocalDateTime merchantTransactionDate;

    private BigDecimal amount;

    private String product;

    private String custAccNo;

    @Column(name = "txnCurrency", length = 3)
    private String txnCurrency;

    private String api;
    private String version;
    private String platform;
    private String subChannel;

    private String udf1;
    private String udf2;
    private String udf3;
    private String udf4;
    private String udf5;

    private String custEmail;
    private String custMobile;

    private Long atomTokenId;
    private Long atomTxnId;

    private BigDecimal surchargeAmount;
    private BigDecimal totalAmount;

    private String clientCode;

    @Column(columnDefinition = "text")
    private String signature;

    private LocalDateTime txnInitDate;
    private LocalDateTime txnCompleteDate;

    private Integer otsBankId;

    private String bankTxnId;

    private String authId;

    private String otsBankName;

    private String cardType;
    private String cardMaskNumber;
    private String scheme;

    private String statusCode;
    private String statusMessage;
    private String statusDescription;

    // ---------- JSON FIELDS ----------
    @Lob
    @Column(name = "request_metadata", columnDefinition = "json")
    private String requestMetadata;

    @Lob
    @Column(name = "response_metadata", columnDefinition = "json")
    private String responseMetadata;

    // ---------- AUDIT FIELDS ----------
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

