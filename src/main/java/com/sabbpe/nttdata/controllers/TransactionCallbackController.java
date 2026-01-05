package com.sabbpe.nttdata.controllers;

import com.sabbpe.nttdata.services.TransactionCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class TransactionCallbackController {

    private final TransactionCallbackService transactionCallbackService;

    @PostMapping("/payment/callback")
    public ResponseEntity<String> handleCallback(@RequestParam("encData") String encData) {

        log.info("Payment callback received");

        try{
            String redirectUrl = transactionCallbackService.processCallback(encData);
            return ResponseEntity.ok(redirectUrl);
        }
        catch(Exception e){
            log.error("❌ Controller caught exception", e);
            return ResponseEntity.ok("error");
        }
    }

}
