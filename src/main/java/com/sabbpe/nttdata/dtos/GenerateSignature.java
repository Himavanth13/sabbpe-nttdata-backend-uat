package com.sabbpe.nttdata.dtos;

import com.sabbpe.nttdata.utils.NttCrypto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenerateSignature {

    private final NttCrypto nttCrypto;

    public String generate() {
//        317159Test@123173821682043500.00INRREFUNDINIT

            return nttCrypto.generateRequestSignature("317159Test@123173821682043500.00INRREFUNDINIT");


    }
}
