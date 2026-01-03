package com.sabbpe.nttdata.controllers;

import com.sabbpe.nttdata.services.TransactionCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class TransactionCallbackUATController {

    private final TransactionCallbackService transactionCallbackService;

    @PostMapping("/payment/callback")
        public String handleCallback(@RequestParam("encData") String encData) {

        return transactionCallbackService.processCallback(encData);

    }

}
