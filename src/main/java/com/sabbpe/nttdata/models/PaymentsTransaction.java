package com.sabbpe.nttdata.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments_transactions")
public class PaymentsTransaction {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;   // DB already generates UUID(), so REMOVE @GeneratedValue

    @Column(name = "client_id", length = 36)
    private String clientId;

    @Column(name = "merchTxnId", length = 128)
    private String merchTxnId;

    @Column(name = "transaction_timestamp")
    private LocalDateTime transactionTimestamp;

    @Column(name = "transaction_token", columnDefinition = "LONGTEXT")
    private String transactionToken;

    @Column(name = "transaction_userid")
    private String transactionUserId;

    @Column(name = "transaction_merchantid")
    private String transactionMerchantId;

    @Column(name = "userId", length = 64)
    private String userId;

    @Column(name = "merchId", length = 64)
    private String merchId;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "merchTxnDate")
    private LocalDateTime merchTxnDate;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "product", length = 64)
    private String product;

    @Column(name = "custAccNo", length = 64)
    private String custAccNo;

    @Column(name = "txnCurrency", length = 3)
    private String txnCurrency;

    @Column(name = "api", length = 32)
    private String api;

    @Column(name = "version", length = 32)
    private String version;

    @Column(name = "platform", length = 32)
    private String platform;

    @Column(name = "subChannel", length = 32)
    private String subChannel;

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

    @Column(name = "custFirstName", length = 255)
    private String custFirstName;

    @Column(name = "custLastName", length = 255)
    private String custLastName;

    @Column(name = "custEmail", length = 255)
    private String custEmail;

    @Column(name = "custMobile", length = 20)
    private String custMobile;

    @Column(name = "custAddr1", length = 255)
    private String custAddr1;

    @Column(name = "custAddr2", length = 255)
    private String custAddr2;

    @Column(name = "custCountry", length = 64)
    private String custCountry;

    @Column(name = "custCity", length = 64)
    private String custCity;

    @Column(name = "custState", length = 64)
    private String custState;

    @Column(name = "custZipCode", length = 32)
    private String custZipCode;

    @Column(name = "custAccIfsc", length = 32)
    private String custAccIfsc;

    @Column(name = "atomTokenId")
    private Long atomTokenId;

    @Column(name = "atomTxnId")
    private Long atomTxnId;

    @Column(name = "surchargeAmount", precision = 12, scale = 2)
    private BigDecimal surchargeAmount;

    @Column(name = "totalAmount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "clientCode", length = 32)
    private String clientCode;

    @Column(name = "signature", columnDefinition = "TEXT")
    private String signature;

    @Column(name = "txnInitDate")
    private LocalDateTime txnInitDate;

    @Column(name = "txnCompleteDate")
    private LocalDateTime txnCompleteDate;

    @Column(name = "otsBankId")
    private Integer otsBankId;

    @Column(name = "bankTxnId", length = 64)
    private String bankTxnId;

    @Column(name = "authId", length = 32)
    private String authId;

    @Column(name = "otsBankName", length = 150)
    private String otsBankName;

    @Column(name = "cardType", length = 32)
    private String cardType;

    @Column(name = "cardMaskNumber", length = 32)
    private String cardMaskNumber;

    @Column(name = "scheme", length = 32)
    private String scheme;

    @Column(name = "statusCode", length = 32)
    private String statusCode;

    @Column(name = "statusMessage", length = 255)
    private String statusMessage;

    @Column(name = "statusDescription", length = 255)
    private String statusDescription;

    @Column(name = "request_metadata", columnDefinition = "LONGTEXT")
    private String requestMetadata;

    @Column(name = "response_metadata", columnDefinition = "LONGTEXT")
    private String responseMetadata;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "qrString", columnDefinition = "TEXT")
    private String qrString;

    @Column(name = "mccCode", length = 32)
    private String mccCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
