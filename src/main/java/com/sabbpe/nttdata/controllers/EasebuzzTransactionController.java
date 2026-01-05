package com.sabbpe.nttdata.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sabbpe.nttdata.dtos.EasebuzzInitiateRequest;
import com.sabbpe.nttdata.services.EasebuzzCallbackService;
import com.sabbpe.nttdata.services.EasebuzzTransactionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/easebuzz")
@RequiredArgsConstructor
public class EasebuzzTransactionController {

    private final EasebuzzTransactionService easebuzzTransactionService;
    private final EasebuzzCallbackService easebuzzCallbackService;

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiatePayment(
            @RequestBody EasebuzzInitiateRequest request) throws Exception {

        log.info("Easebuzz initiate request received for txnid: {}", request.getTxnid());

        Map<String, Object> response = easebuzzTransactionService.initiatePayment(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void handleCallback(
            @RequestParam Map<String, String> payload,
            HttpServletResponse response
    ) throws IOException {
        try {
            log.info(" Easebuzz callback received for txnid: {}", payload.get("txnid"));

            String redirectUrl = easebuzzCallbackService.processCallback(payload);

            log.info("↪ Redirecting to: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error(" Callback processing failed: {}", e.getMessage(), e);

            try {
                String frontendUrl = payload.getOrDefault("udf2", "http://localhost:5173");
                if (frontendUrl.endsWith("/")) {
                    frontendUrl = frontendUrl.substring(0, frontendUrl.length() - 1);
                }

                String errorUrl = frontendUrl + "/payment-result"
                        + "?status=error"
                        + "&txnid=" + payload.getOrDefault("txnid", "unknown")
                        + "&error=" + URLEncoder.encode(
                        "System error: " + e.getMessage(),
                        StandardCharsets.UTF_8);

                response.sendRedirect(errorUrl);
                log.info("↪️  Fallback redirect: {}", errorUrl);

            } catch (Exception fallbackError) {
                log.error(" Fallback redirect failed: {}", fallbackError.getMessage());
            }

        }
    }
}
