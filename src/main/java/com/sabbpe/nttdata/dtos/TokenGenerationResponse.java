package com.sabbpe.nttdata.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TokenGenerationResponse {

    @JsonProperty("transaction_token")
    private String transactionToken;

    @JsonProperty("transaction_id")
    private String transactionId;  // For tracking/debugging

    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;  // Client needs to know expiry

    @JsonProperty("status")
    private String status;  // "success" or "error"
}
