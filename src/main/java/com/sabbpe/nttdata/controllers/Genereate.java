package com.sabbpe.nttdata.controllers;

import com.sabbpe.nttdata.dtos.GenerateSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/man")
public class Genereate {

    @Autowired
    private GenerateSignature generateSignature;
    @GetMapping("/generate")
    private String generate() {
        return generateSignature.generate();
    }
}
