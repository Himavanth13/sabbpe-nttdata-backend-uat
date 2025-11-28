package com.sabbpe.nttdata.services;

import com.sabbpe.nttdata.dtos.TransactionRequest;
import com.sabbpe.nttdata.dtos.TransactionStatusRequest;
import com.sabbpe.nttdata.utils.NttCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.IllegalBlockSizeException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrasactionStatusService {

    private final NttCrypto nttCrypto;

    @Value("${ndps.transaction.status-api}")
    private String TRASACTION_STATUS_URL;

    public String transactionStatusPayload(TransactionStatusRequest request) {
        log.info("request payload : {}",request);
//        [merchID +
//                Password +
//                merchTxnID +
//                amount +
//                txnCurrency + api
//]
        //extracting required details to generate signature
        Long merchID=request.getPayInstrument().getMerchDetails().getMerchId();
        String password=request.getPayInstrument().getMerchDetails().getPassword();
        String merchTxnID=request.getPayInstrument().getMerchDetails().getMerchTxnId();
        Double amount=request.getPayInstrument().getPayDetails().getAmount();
        String txnCurrency=request.getPayInstrument().getPayDetails().getTxnCurrency();
        String api=request.getPayInstrument().getHeadDetails().getApi();

        String raw=merchID+password+merchTxnID+amount+txnCurrency+api;
        log.info("raw value : {}", raw);

        //2.generating signature
        String generatedReqSignature= nttCrypto.generateRequestSignature(raw);

        generatedReqSignature=generatedReqSignature.toLowerCase();
        log.info("generatedReqSignature : {}",generatedReqSignature);
        //3. setting the generated signature
        request.getPayInstrument().
                getPayDetails()
                .setSignature(generatedReqSignature);

        //4.encrypt payload
        String enc=nttCrypto.encryptRequest(request);
        log.info("encrypted data {}",enc);

        //5. ready the url
        String url = UriComponentsBuilder.fromUriString(TRASACTION_STATUS_URL)
                .queryParam("merchId", merchID)
                .queryParam("encData", enc)
                .build()
                .encode()
                .toUriString();

        log.info("FINAL TransactionStatus URL = {}", url);

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
//        if(res!=null && res.contains("encData")) {
//            String input = response.getBody();
//            int start = input.indexOf("encData=") + "encData=".length();
//            int end = input.indexOf("&merchId");
//
//            String encData = input.substring(start, end);
//            return nttCrypto.decryptResponse(encData);
//        }
        try {
            return nttCrypto.decryptResponse(res);
        }catch (Exception e) {

            log.info("invalid response to decrypt");
        }
        return res;

    }

}
