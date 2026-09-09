package com.smartomni.integration.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * UC-18/FR-038: Ma hoa App Secret bang AES-256 truoc khi luu vao Database.
 * TODO production: doi qua mot Key Management Service (AWS KMS/HashiCorp Vault)
 * thay vi luu secret key truc tiep trong application.yml.
 */
@Service
public class AesEncryptionService {

    @Value("${smartomni.encryption.aes-key}")
    private String aesKeyBase64;

    private static final String ALGORITHM = "AES";

    public String encrypt(String plainText) {
        try {
            SecretKeySpec key = new SecretKeySpec(Base64.getDecoder().decode(aesKeyBase64), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Loi ma hoa App Secret", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            SecretKeySpec key = new SecretKeySpec(Base64.getDecoder().decode(aesKeyBase64), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Loi giai ma App Secret", e);
        }
    }
}
