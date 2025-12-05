package com.sabbpe.nttdata.utils;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AESUtil {

    // ---------------------- KEY + IV GENERATION ------------------------
//
//    // Generate 256-bit AES Key (Base64 encoded)
//    public static String generateAESKey256() throws Exception {
//        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//        keyGen.init(256); // 256-bit key
//        SecretKey secretKey = keyGen.generateKey();
//        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
//    }
//
//    // Generate 16-byte IV (Base64 encoded)
//    public static String generateIV16() {
//        byte[] iv = new byte[16]; // AES block size = 16 bytes
//        new SecureRandom().nextBytes(iv);
//        return Base64.getEncoder().encodeToString(iv);
//
//    }

    // ---------------------- ENCRYPT ------------------------

    public static String encrypt(String plainText, String base64Key, String base64IV) throws Exception {

        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        byte[] ivBytes = Base64.getDecoder().decode(base64IV);
//         byte[] keyBytes=base64Key.getBytes();
//         byte[] ivBytes=base64IV.getBytes();

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);

    }

    // ---------------------- DECRYPT ------------------------

    public static String decrypt(String encryptedText, String base64Key, String base64IV) throws Exception {

        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        byte[] ivBytes = Base64.getDecoder().decode(base64IV);
//          byte[] keyBytes=base64Key.getBytes();
//          byte[] ivBytes=base64IV.getBytes();
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] decoded = Base64.getDecoder().decode(encryptedText);
        byte[] decrypted = cipher.doFinal(decoded);

        return new String(decrypted);


    }
}
