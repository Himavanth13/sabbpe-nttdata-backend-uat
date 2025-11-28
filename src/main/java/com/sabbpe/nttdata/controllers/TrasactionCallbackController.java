package com.sabbpe.nttdata.controllers;

import com.sabbpe.nttdata.utils.NttCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class TrasactionCallbackController {

    private final NttCrypto nttCrypto;

    @PostMapping("/initiatepaymentcallback")
    public void handleCallback(@RequestParam("encData")String encData) {
        try {
            log.info("📥 CALLBACK RECEIVED");
            log.info("callback data : {}",encData);

            String callback =
                    nttCrypto.decryptResponse(encData);

            log.info("📥 CALLBACK RECEIVED");
            log.info("callback data : {}",callback);




            // ❗ validate signature
            String generatedResponseSignature = nttCrypto.generateRequestSignature(
                    callback
            );

//
//            if (!isValidSignature) {
//                System.out.println("❌ Callback signature INVALID");
//                return ResponseEntity.ok("INVALID SIGNATURE");
//            }

//            return ResponseEntity.ok("OK"); // NDPS expects HTTP 200

        } catch (Exception e) {
            e.printStackTrace();
//            return ResponseEntity.ok("FAILED");

        }
    }
}
