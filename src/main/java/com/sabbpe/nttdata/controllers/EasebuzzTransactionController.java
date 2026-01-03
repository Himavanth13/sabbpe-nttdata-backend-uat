package com.sabbpe.nttdata.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sabbpe.nttdata.dtos.EasebuzzInitiateRequest;
import com.sabbpe.nttdata.services.EasebuzzTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/easebuzz")
@RequiredArgsConstructor
public class EasebuzzTransactionController {

    private final EasebuzzTransactionService easebuzzTransactionService;

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiatePayment(
            @RequestBody EasebuzzInitiateRequest request) throws Exception {

        log.info("Easebuzz initiate request received for txnid: {}", request.getTxnid());

        Map<String, Object> response = easebuzzTransactionService.initiatePayment(request);

        return ResponseEntity.ok(response);
    }
}
