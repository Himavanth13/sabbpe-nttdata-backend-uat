package com.sabbpe.nttdata.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "easebuzz_processor_transaction_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EasebuzzTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "master_transaction_id", nullable = false, columnDefinition = "CHAR(36)")
    private String masterTransactionId;

    // Transaction Details
    @Column(name = "txnid", length = 128)
    private String txnid;

    @Column(name = "easepay_id", length = 64)
    private String easepayId;

    @Column(name = "access_key", length = 128)
    private String accessKey;

    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;

    @Column(name = "bank_ref_num", length = 64)
    private String bankRefNum;

    // Amount Details
    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "net_amount_debit", precision = 12, scale = 2)
    private BigDecimal netAmountDebit;

    @Column(name = "deduction_percentage", precision = 6, scale = 2)
    private BigDecimal deductionPercentage;

    @Column(name = "cashback_percentage", precision = 6, scale = 2)
    private BigDecimal cashbackPercentage;

    // Status Details
    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @Column(name = "error_text", length = 255)
    private String errorText;

    @Column(name = "unmapped_status", length = 64)
    private String unmappedStatus;

    // Customer Details
    @Column(name = "customer_first_name", length = 255)
    private String customerFirstName;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_phone", length = 32)
    private String customerPhone;

    // Payment Details
    @Column(name = "payment_source", length = 32)
    private String paymentSource;

    @Column(name = "mode", length = 32)
    private String mode;

    // Card Details
    @Column(name = "card_type", length = 32)
    private String cardType;

    @Column(name = "card_category", length = 32)
    private String cardCategory;

    @Column(name = "card_number", length = 32)
    private String cardNumber;

    @Column(name = "name_on_card", length = 255)
    private String nameOnCard;

    // Bank Details
    @Column(name = "issuing_bank", length = 64)
    private String issuingBank;

    @Column(name = "bank_name", length = 64)
    private String bankName;

    @Column(name = "bank_code", length = 32)
    private String bankCode;

    @Column(name = "upi_va", length = 64)
    private String upiVa;

    // Product & Merchant Details
    @Column(name = "product_info", length = 255)
    private String productInfo;

    @Column(name = "auth_code", length = 64)
    private String authCode;

    @Column(name = "pg_type", length = 64)
    private String pgType;

    @Column(name = "merchant_key", length = 64)
    private String merchantKey;

    @Column(name = "merchant_logo", length = 255)
    private String merchantLogo;

    // URLs
    @Column(name = "success_url", columnDefinition = "TEXT")
    private String successUrl;

    @Column(name = "failure_url", columnDefinition = "TEXT")
    private String failureUrl;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    // UDF Fields
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

    // Security
    @Column(name = "hash", columnDefinition = "TEXT")
    private String hash;

    @Column(name = "added_on")
    private LocalDateTime addedOn;

    // JSON Payloads
    @Column(name = "request_payload", columnDefinition = "LONGTEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "LONGTEXT")
    private String responsePayload;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
