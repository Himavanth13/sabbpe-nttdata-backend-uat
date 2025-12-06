package com.sabbpe.nttdata.controllers;

import com.sabbpe.nttdata.dtos.TransactionRequest;
import com.sabbpe.nttdata.dtos.TransactionSuccessResponse;
import com.sabbpe.nttdata.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/PaymentProcess")
    public TransactionSuccessResponse initiate(@RequestBody TransactionRequest request) {
        return transactionService.initiate(request);
    }
}
