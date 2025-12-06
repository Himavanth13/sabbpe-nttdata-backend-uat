package com.sabbpe.nttdata.controllers;

import com.sabbpe.nttdata.dtos.RefundRequest;
import com.sabbpe.nttdata.services.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.IllegalBlockSizeException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/refund")
    public String refund(@RequestBody RefundRequest request) throws Exception {
        return refundService.refund(request);
    }

}
