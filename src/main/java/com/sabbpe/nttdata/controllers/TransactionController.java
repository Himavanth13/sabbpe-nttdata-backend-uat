package com.sabbpe.nttdata.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.nttdata.dtos.TransactionRequest;
import com.sabbpe.nttdata.dtos.TransactionSuccessResponse;
import com.sabbpe.nttdata.services.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/PaymentProcess")
    public TransactionSuccessResponse initiate(@RequestBody TransactionRequest request) throws JsonProcessingException {

        log.info("Payment initiation request received");
        return transactionService.initiate(request);
    }
}
