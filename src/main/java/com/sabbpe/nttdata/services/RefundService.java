package com.sabbpe.nttdata.services;

import com.sabbpe.nttdata.dtos.RefundRequest;
import com.sabbpe.nttdata.utils.NttCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.IllegalBlockSizeException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    @Value("${ndps.refund.url}")
    private String refundUrl;   //  DO NOT MAKE FINAL

    private final NttCrypto nttCrypto;

    public String refund(RefundRequest request) throws Exception {

        // 1. Validate request to avoid NPE
        if (request.getPayInstrument() == null ||
                request.getPayInstrument().getPayDetails() == null ||
                request.getPayInstrument().getMerchDetails() == null) {
            throw new IllegalArgumentException("Invalid refund request structure");
        }


        // 2. Generate SIGNATURE dynamically (DO NOT hardcode)
        Integer merchId=request.getPayInstrument().getMerchDetails().getMerchId();
        String merchTxnId=request.getPayInstrument().getMerchDetails().getMerchTxnId();
        String password=request.getPayInstrument().getMerchDetails().getPassword();
        Double amoutn=request.getPayInstrument().getPayDetails().getTotalRefundAmount();

        //"317159Test@123111121685555100.25INRREFUNDINIT"
        String raw= merchId+password+merchTxnId+amoutn+"INRREFUNDINIT";
        String signature = nttCrypto.generateRequestSignature(raw);
        log.info("signature "+signature.toLowerCase());
        request.getPayInstrument()
                .getPayDetails()
                .setSignature(signature.toLowerCase());

        //  3. Encrypt the payload
        String encryptedPayload = nttCrypto.encryptRequest(request);

        Integer merchantId =
                request.getPayInstrument()
                        .getMerchDetails()
                        .getMerchId();

        // 4. Build FINAL REFUND URL WITH encData
        String url = UriComponentsBuilder.fromUriString(refundUrl)
                .queryParam("merchId", merchantId)
                .queryParam("encData", encryptedPayload)
                .build()
                .encode()
                .toUriString();

        log.info("FINAL REFUND URL = {}", url);

        // 5. POST to external API
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        log.info("REFUND API STATUS  = {}", response.getStatusCode());
        log.info("REFUND API BODY    = {}", response.getBody());
        String res=response.getBody();
        if(res!=null && res.contains("encData")) {
            String input = response.getBody();
            int start = input.indexOf("encData=") + "encData=".length();
            int end = input.indexOf("&merchId");

            String encData = input.substring(start, end);
            return nttCrypto.decryptResponse(encData);
        }
        return res;

    }

}
