package com.sabbpe.nttdata.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class TransactionSuccessResponse {

    private Long atomTokenId;
    private ResponseDetails responseDetails;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResponseDetails {
        private String txnMessage;
        private String txnStatusCode;
        private String txnDescription;
    }
}
