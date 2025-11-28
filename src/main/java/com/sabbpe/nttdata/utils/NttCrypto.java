package com.sabbpe.nttdata.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.nttdata.dtos.TransactionCallbackResponse;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;

@Slf4j
public class NttCrypto {

    private final byte[] reqKey;
    private final byte[] reqSalt;
    private final byte[] resKey;
    private final byte[] resSalt;
    private final String reqHashKey;
    private final String resHashKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final byte[] iv = new byte[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
    };

    private final byte[] derivedReqKey;
    private final byte[] derivedResKey;

    public NttCrypto(String reqEncKey, String reqSalt, String resEncKey, String resSalt,
                      String reqHashKey, String resHashKey) {
        this.reqKey = reqEncKey.getBytes(StandardCharsets.US_ASCII);
        this.reqSalt = reqSalt.getBytes(StandardCharsets.US_ASCII);
        this.resKey = resEncKey.getBytes(StandardCharsets.US_ASCII);
        this.resSalt = resSalt.getBytes(StandardCharsets.US_ASCII);
        this.reqHashKey = reqHashKey;
        this.resHashKey = resHashKey;

        this.derivedReqKey = deriveKey(this.reqKey, this.reqSalt);
        this.derivedResKey = deriveKey(this.resKey, this.resSalt);
    }

    private byte[] deriveKey(byte[] key, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(new String(key).toCharArray(), salt, 65536, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Unable to derive encryption key", e);
        }
    }

    /** 🔐 Encrypt request payload */
    public String encryptRequest(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(derivedReqKey, "AES"), new IvParameterSpec(iv));

            byte[] encrypted = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encrypted).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }


    public <T> T decryptResponse(String hexString, Class<T> clazz) {
        try {
            byte[] encrypted = hexToBytes(hexString);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(derivedResKey, "AES"),
                    new IvParameterSpec(iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            String json = new String(decrypted, StandardCharsets.UTF_8);

            try {
                return objectMapper.readValue(json, clazz);
            } catch (Exception mappingError) {
                throw new RuntimeException("JSON Mapping failed: " + json, mappingError);
            }

        } catch (Exception cryptoError) {
            throw new RuntimeException("AES Decryption failed", cryptoError);
        }
    }

    public String decryptResponse(String hexString) throws Exception {
        try {
            byte[] encrypted = hexToBytes(hexString);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(derivedResKey, "AES"),
                    new IvParameterSpec(iv)
            );

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("AES Decryption failed", e);
        }
    }



//    /** 🔓 Decrypt NDPS encrypted response */
//    public <T> T decryptResponse(String hexString, Class<T> clazz) {
//        try {
//            byte[] encrypted = hexToBytes(hexString);
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//            cipher.init(Cipher.DECRYPT_MODE,
//                    new SecretKeySpec(derivedResKey, "AES"), new IvParameterSpec(iv));
//
//            byte[] decrypted = cipher.doFinal(encrypted);
//            String json = new String(decrypted, StandardCharsets.UTF_8);
//            return objectMapper.readValue(json, clazz);
//        } catch (Exception e) {
//            throw new RuntimeException("Decryption failed", e);
//        }
//    }

    /** 🧾 Status API HMAC Signature */
    public String generateResSignature(String merchId, String merchTxnId, double amount, String currency) {
        try {
            String formattedAmount = String.format("%.2f", amount);
            String raw = merchId + merchTxnId + formattedAmount + currency;
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            sha512_HMAC.init(new SecretKeySpec(resHashKey.getBytes(), "HmacSHA512"));
            return bytesToHex(sha512_HMAC.doFinal(raw.getBytes())).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Status signature failed", e);
        }
    }


    public String generateReqSignature(String merchId, String merchTxnId, double amount, String currency) {
        try {
            String formattedAmount = String.format("%.2f", amount);
            String raw = merchId + merchTxnId + formattedAmount + currency;
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            sha512_HMAC.init(new SecretKeySpec(reqHashKey.getBytes(), "HmacSHA512"));
            return bytesToHex(sha512_HMAC.doFinal(raw.getBytes())).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Status signature failed", e);
        }
    }
//MerchId+Transaction Password+MerchTxnID+Amount+Currency+ API
    public String generateRequestSignature(String raw) {
        try {
//            String formattedAmount = String.format("%.2f", amount);
//            String raw = merchId + merchTxnId + formattedAmount + currency;
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            sha512_HMAC.init(new SecretKeySpec(reqHashKey.getBytes(), "HmacSHA512"));
            return bytesToHex(sha512_HMAC.doFinal(raw.getBytes())).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Status signature failed", e);
        }
    }

    public String generateResponseSignature(String raw) {

        try {
//            String formattedAmount = String.format("%.2f", amount);
//            String raw = merchId + merchTxnId + formattedAmount + currency;
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            sha512_HMAC.init(new SecretKeySpec(resHashKey.getBytes(), "HmacSHA512"));
            return bytesToHex(sha512_HMAC.doFinal(raw.getBytes())).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Status signature failed", e);
        }

    }


    /** 🛡 Callback signature verification */
    public boolean verifyCallbackSignature(String calculated, String received) {
        return calculated != null && calculated.equalsIgnoreCase(received);
    }

    /** UTIL */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        return out;
    }

//========================================================================================================================



    public boolean validateCallbackSignature(TransactionCallbackResponse callback) {
        try {
            // Extract and format fields
            String merchId = String.valueOf(callback.getPayInstrument().getMerchDetails().getMerchId());
            String merchTxnId = String.valueOf(callback.getPayInstrument().getMerchDetails().getMerchTxnId());

            // ✅ CRITICAL: Format amount with 2 decimals
            double amountValue = callback.getPayInstrument().getPayDetails().getAmount();
            String amount = String.format("%.2f", amountValue);

            String currency = callback.getPayInstrument().getPayDetails().getTxnCurrency();

            // Build raw string
            String rawData =  merchId+merchTxnId + amount + currency;

            // Generate HMAC-SHA512 signature
            String generatedSignature = generateCallbackSignatureHMAC(rawData);
            String receivedSignature = callback.getPayInstrument().getPayDetails().getSignature();
            log.info("generatedSignature :"+generatedSignature);
            log.info("recieved signature :"+receivedSignature);

            return generatedSignature.equalsIgnoreCase(receivedSignature);

        } catch (Exception e) {
            log.error("❌ Callback signature verification failed", e);
            return false;
        }
    }

    private String generateCallbackSignatureHMAC(String rawData) {
        try {
            Mac hmacSha512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(
                    resHashKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            );
            hmacSha512.init(secretKey);
            byte[] signature = hmacSha512.doFinal(rawData.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(signature).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA512 signature generation failed", e);
        }
    }



    private String encryptAESHex(String data, byte[] key, byte[] iv) throws Exception {

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new IvParameterSpec(iv)
        );

        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        return bytesToHex(encrypted);
    }

//    private String bytesToHex(byte[] bytes) {
//        StringBuilder sb = new StringBuilder();
//        for (byte b : bytes) {
//            sb.append(String.format("%02X", b));
//        }
//        return sb.toString();
//    }
}
