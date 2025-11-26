package com.sabbpe.nttdata.config;

import com.sabbpe.nttdata.utils.NttCrypto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NttCryptoConfig {

    @Bean
    public NttCrypto nttCrypto(
            @Value("${ndps.reqEncKey}") String reqEncKey,
            @Value("${ndps.reqSalt}") String reqSalt,
            @Value("${ndps.resEncKey}") String resEncKey,
            @Value("${ndps.resSalt}") String resSalt,
            @Value("${ndps.reqHashKey}") String reqHashKey,
            @Value("${ndps.resHashKey}") String resHashKey
    ) {
        return new NttCrypto(reqEncKey, reqSalt, resEncKey, resSalt, reqHashKey, resHashKey);
    }
}
