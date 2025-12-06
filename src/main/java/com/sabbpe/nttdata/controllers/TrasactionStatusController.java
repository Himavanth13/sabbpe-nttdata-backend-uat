package com.sabbpe.nttdata.controllers;

import com.sabbpe.nttdata.dtos.TransactionStatusRequest;
import com.sabbpe.nttdata.services.TrasactionStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class TrasactionStatusController {

    private final TrasactionStatusService trasactionStatusService;

    @PostMapping("/PaymentStatus")
    public String getTransaction(@RequestBody TransactionStatusRequest transactionStatusRequest) {

       return trasactionStatusService.transactionStatusPayload(transactionStatusRequest);

    }

}
