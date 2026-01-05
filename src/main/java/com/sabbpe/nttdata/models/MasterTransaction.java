package com.sabbpe.nttdata.models;

import com.sabbpe.nttdata.enums.TransactionStatus;
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
@Table(
        name = "master_transactions",
        indexes = {
                @Index(name = "idx_processor", columnList = "processor"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_master_client", columnList = "client_id"),
                @Index(name = "idx_master_created", columnList = "initiated_at")
        }
)
public class MasterTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "order_reference", length = 128, nullable = false, unique = true)
    private String orderReference;

    @Column(name = "transaction_token", columnDefinition = "LONGTEXT")
    private String transactionToken;

    @Column(name = "transaction_timestamp")
    private LocalDateTime transactionTimestamp;

    @Column(name = "transaction_userid", length = 64)
    private String transactionUserId;

    @Column(name = "transaction_merchantid", length = 64)
    private String transactionMerchantId;

    @Column(name = "client_id", length = 36, nullable = false)
    private String clientId;

    @Column(name = "amount_requested", precision = 12, scale = 2, nullable = false)
    private BigDecimal amountRequested;

    @Column(name = "amount_final", precision = 12, scale = 2)
    private BigDecimal amountFinal;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private TransactionStatus status;

    @Column(name = "processor", length = 32, nullable = false)
    private String processor;

    @CreationTimestamp
    @Column(name = "initiated_at", nullable = false, updatable = false)
    private LocalDateTime initiatedAt;

    @UpdateTimestamp
    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
}
