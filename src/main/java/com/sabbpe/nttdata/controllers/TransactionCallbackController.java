package com.sabbpe.nttdata.controllers;

import com.sabbpe.nttdata.services.TransactionCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class TransactionCallbackController {

    private final TransactionCallbackService transactionCallbackService;

    @PostMapping("/payment/callback")
        public String handleCallback(@RequestParam("encData") String encData) {

        return transactionCallbackService.callback(encData);

    }

}
