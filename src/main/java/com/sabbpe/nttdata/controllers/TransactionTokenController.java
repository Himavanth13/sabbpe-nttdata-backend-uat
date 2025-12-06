package com.sabbpe.nttdata.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.nttdata.services.TransactionTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TransactionTokenController {

    private final TransactionTokenService transactionTokenService;

    @PostMapping("/PaymentGenerateToken")
    public String generateTransactionToken(
         @RequestBody Map<String, Object> body
    ) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String prettyJson = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(body);
        log.info("generate token request payload : {}",prettyJson);
        String transactionUserId=String.valueOf(body.get("transaction_userid"));
        String transactionMerchantId=String.valueOf(body.get("transaction_merchantid"));
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
