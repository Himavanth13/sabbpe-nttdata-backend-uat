package com.sabbpe.nttdata.controllers;

import com.sabbpe.nttdata.services.TransactionTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TransactionTokenController {

    private final TransactionTokenService transactionTokenService;

    @PostMapping("/generate/transactiontoken")
    public String generateTransactionToken(
            @RequestHeader("transaction_userid") String transactionUserId,
            @RequestHeader("transaction_merchantid") String transactionMerchantId,
            @RequestBody Map<String, Object> body
    ) throws Exception {

        String clientId   = String.valueOf(body.get("client_Id"));          // note the capital I
        String timestamp  = String.valueOf(body.get("transaction_timestamp"));

        return transactionTokenService.encryptTransaction(
                transactionUserId,
                transactionMerchantId,
                clientId,
                timestamp
        );
    }
}
