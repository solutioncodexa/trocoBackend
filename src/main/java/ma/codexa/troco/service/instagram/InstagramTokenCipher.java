package ma.codexa.troco.service.instagram;

import ma.codexa.troco.config.InstagramOAuthProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** Chiffrement AES-GCM des jetons Instagram et signature HMAC du paramètre {@code state}, avec la même clé maître. */
@Component
public class InstagramTokenCipher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final InstagramOAuthProperties props;

    public InstagramTokenCipher(InstagramOAuthProperties props) {
        this.props = props;
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key("enc"), "AES"), new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = Arrays.copyOf(iv, iv.length + ct.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Chiffrement impossible", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] in = Base64.getDecoder().decode(encoded);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key("enc"), "AES"), new GCMParameterSpec(128, Arrays.copyOf(in, 12)));
            return new String(c.doFinal(in, 12, in.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Jeton illisible (clé modifiée ?)", e);
        }
    }

    /** @return signature base64url du texte. */
    public String sign(String text) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key("sig"), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean verify(String text, String signature) {
        return signature != null && MessageDigest.isEqual(
                sign(text).getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] key(String purpose) throws GeneralSecurityException {
        return MessageDigest.getInstance("SHA-256").digest((purpose + ":" + props.tokenKey()).getBytes(StandardCharsets.UTF_8));
    }
}
