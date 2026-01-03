package com.sabbpe.nttdata.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class HashUtil {

    public static String sha512Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating SHA-512 hash", e);
        }
    }
}