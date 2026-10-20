package ke.shulehub.payments.internal;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import ke.shulehub.shared.config.ShuleHubProperties;

/**
 * AES-256-GCM for M-Pesa credentials at rest. The key comes from {@code shulehub.credentials-key}
 * (base64, 32 bytes); in production it should come from a secrets manager.
 */
@Component
public class CredentialCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public CredentialCipher(ShuleHubProperties properties) {
        if (properties.credentialsKey() == null || properties.credentialsKey().isBlank()) {
            throw new IllegalStateException("Set shulehub.credentials-key (base64-encoded 32-byte key)");
        }
        byte[] raw = Base64.getDecoder().decode(properties.credentialsKey().trim());
        if (raw.length != 32) {
            throw new IllegalStateException("shulehub.credentials-key must decode to 32 bytes, got " + raw.length);
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not encrypt credential", ex);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] data = Base64.getDecoder().decode(encoded);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
            byte[] plain = cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not decrypt credential; was the key changed?", ex);
        }
    }
}
