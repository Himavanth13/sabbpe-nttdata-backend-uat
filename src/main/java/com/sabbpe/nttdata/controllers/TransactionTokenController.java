package com.sabbpe.nttdata.controllers;


import com.sabbpe.nttdata.projection.ClientCryptoProjection;
import com.sabbpe.nttdata.services.ClientProfileService;
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
    public String getCryptoKeys(
            @RequestHeader("transaction_userid") String transactionUserId,
            @RequestHeader("transaction_merchantid") String transactionMerchantId,
            @RequestBody Map<String,Object> map

    ) throws Exception {
        String client_id=String.valueOf(map.get("client_Id"));
        String timedate= String.valueOf(map.get("transaction_timestamp"));
       return transactionTokenService.encryptTransaction(transactionUserId,transactionMerchantId,client_id,timedate);

    }
}


