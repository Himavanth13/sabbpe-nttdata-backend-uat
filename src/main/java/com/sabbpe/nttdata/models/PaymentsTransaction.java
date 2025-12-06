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
    @Column(nullable = false, length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "merchTxnId")
    private String merchTxnId;

    @Column(name = "merchant_transaction_timestamp")
    private LocalDateTime merchantTransactionTimestamp;

    @Column(name = "transaction_token", columnDefinition = "LONGTEXT")
    private String transactionToken;

    @Column(name = "userId")
    private String userId;

    @Column(name = "merchId")
    private String merchId;

    @Column(name = "password")
    private String password;

    @Column(name = "merchTxnDate")
    private LocalDateTime merchTxnDate;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "product")
    private String product;

    @Column(name = "custAccNo")
    private String custAccNo;

    @Column(name = "txnCurrency")
    private String txnCurrency;

    @Column(name = "api")
    private String api;

    @Column(name = "version")
    private String version;

    @Column(name = "platform")
    private String platform;

    @Column(name = "subChannel")
    private String subChannel;

    @Column(name = "udf1")
    private String udf1;

    @Column(name = "udf2")
    private String udf2;

    @Column(name = "udf3")
    private String udf3;

    @Column(name = "udf4")
    private String udf4;

    @Column(name = "udf5")
    private String udf5;

    @Column(name = "udf6")
    private String udf6;

    @Column(name = "udf7")
    private String udf7;

    @Column(name = "udf8")
    private String udf8;

    @Column(name = "udf9")
    private String udf9;

    @Column(name = "udf10")
    private String udf10;

    @Column(name = "custFirstName")
    private String custFirstName;

    @Column(name = "custLastName")
    private String custLastName;

    @Column(name = "custEmail")
    private String custEmail;

    @Column(name = "custMobile")
    private String custMobile;

    @Column(name = "custAddr1")
    private String custAddr1;

    @Column(name = "custAddr2")
    private String custAddr2;

    @Column(name = "custCountry")
    private String custCountry;

    @Column(name = "custCity")
    private String custCity;

    @Column(name = "custState")
    private String custState;

    @Column(name = "custZipCode")
    private String custZipCode;

    @Column(name = "custAccIfsc")
    private String custAccIfsc;

    @Column(name = "atomTokenId")
    private Long atomTokenId;

    @Column(name = "atomTxnId")
    private Long atomTxnId;

    @Column(name = "surchargeAmount")
    private BigDecimal surchargeAmount;

    @Column(name = "totalAmount")
    private BigDecimal totalAmount;

    @Column(name = "clientCode")
    private String clientCode;

    @Column(name = "signature", columnDefinition = "TEXT")
    private String signature;

    @Column(name = "txnInitDate")
    private LocalDateTime txnInitDate;

    @Column(name = "txnCompleteDate")
    private LocalDateTime txnCompleteDate;

    @Column(name = "otsBankId")
    private Integer otsBankId;

    @Column(name = "bankTxnId")
    private String bankTxnId;

    @Column(name = "authId")
    private String authId;

    @Column(name = "otsBankName")
    private String otsBankName;

    @Column(name = "cardType")
    private String cardType;

    @Column(name = "cardMaskNumber")
    private String cardMaskNumber;

    @Column(name = "scheme")
    private String scheme;

    @Column(name = "statusCode")
    private String statusCode;

    @Column(name = "statusMessage")
    private String statusMessage;

    @Column(name = "statusDescription")
    private String statusDescription;

    @Column(name = "request_metadata", columnDefinition = "LONGTEXT")
    private String requestMetadata;

    @Column(name = "response_metadata", columnDefinition = "LONGTEXT")
    private String responseMetadata;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "qrString", columnDefinition = "TEXT")
    private String qrString;

    @Column(name = "mccCode")
    private String mccCode;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
