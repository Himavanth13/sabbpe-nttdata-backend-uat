package com.sabbpe.nttdata.models;

import com.sabbpe.nttdata.enums.PaymentMethod;
import com.sabbpe.nttdata.enums.PaymentProvider;
import com.sabbpe.nttdata.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "payment_method_id", columnList = "payment_method"),
                @Index(name = "instrument_id", columnList = "payment_provider"),
                @Index(name = "idx_client_txn", columnList = "client_id, merchant_order_id"),
                @Index(name = "idx_txn_status", columnList = "status"),
                @Index(name = "idx_provider_txn", columnList = "provider_txn_id")
        }
)
@Data
public class Transaction {

    // ✅ AUTO UUID (HIBERNATE 6 / SPRING BOOT 3+)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", length = 36, nullable = false, updatable = false)
    private String transactionId;

    // ✅ Foreign Key
    @Column(name = "client_id", length = 36, nullable = false)
    private String clientId;

    @Column(name = "merchant_order_id", length = 128, nullable = false)
    private String merchantOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.UPI;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider", nullable = false)
    private PaymentProvider paymentProvider;

    // ✅ MONEY MUST BE BigDecimal
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status = TransactionStatus.INITIATED;

    @Column(name = "provider_txn_id", length = 255)
    private String providerTxnId;

    @Column(name = "provider_order_id", length = 255)
    private String providerOrderId;

    @Column(name = "auth_code", length = 32)
    private String authCode;

    @Column(name = "gateway_reference", length = 255)
    private String gatewayReference;

    @Column(name = "customer_ip", length = 45)
    private String customerIp;

    @Column(name = "customer_user_agent", length = 512)
    private String customerUserAgent;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    // ✅ MUST BE BigDecimal (THIS FIXES YOUR ERROR)
    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    // ✅ JSON FIELDS
    @Lob
    @Column(name = "risk_flags", columnDefinition = "longtext")
    private String riskFlags;

    @Lob
    @Column(name = "request_metadata", columnDefinition = "longtext")
    private String requestMetadata;

    @Lob
    @Column(name = "response_metadata",columnDefinition = "LONGTEXT")
    private String responseMetadata;

    // ✅ AUTO TIMESTAMPS
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
