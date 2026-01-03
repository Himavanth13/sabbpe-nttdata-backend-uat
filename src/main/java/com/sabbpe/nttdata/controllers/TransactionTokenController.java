package com.sabbpe.nttdata.controllers;

import com.sabbpe.nttdata.dtos.TokenGenerationRequest;
import com.sabbpe.nttdata.services.TransactionTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TransactionTokenController {

    private final TransactionTokenService transactionTokenService;

    @PostMapping("/PaymentGenerateToken")
    public ResponseEntity<String> generateToken(
            @RequestBody @Valid TokenGenerationRequest request) throws Exception {

        log.info("Token generation request for client: {}", request.getClientId());

        String token = transactionTokenService.generateToken(request);

        return ResponseEntity.ok(token);
    }
}
