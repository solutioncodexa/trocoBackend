package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.entity.StoreApiKey;
import ma.codexa.troco.repository.StoreApiKeyRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StoreApiKeyService {

    private final StoreApiKeyRepository storeApiKeyRepository;
    private final PlanEntitlementService planEntitlementService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        Long fid = TenantContext.requireFournisseurId();
        return storeApiKeyRepository.findByFournisseurIdOrderByCreatedAtDesc(fid).stream()
                .map(this::toPublic)
                .toList();
    }

    @Transactional
    public Map<String, Object> create(String name, String scopes) {
        planEntitlementService.assertApiKeysAllowed();
        Long fid = TenantContext.requireFournisseurId();
        byte[] raw = new byte[24];
        secureRandom.nextBytes(raw);
        String secret = "mk_" + HexFormat.of().formatHex(raw);
        String prefix = secret.substring(0, 12);

        StoreApiKey key = new StoreApiKey();
        key.setFournisseurId(fid);
        key.setName(name != null && !name.isBlank() ? name.trim() : "API key");
        key.setKeyPrefix(prefix);
        key.setKeyHash(sha256(secret));
        key.setScopes(scopes != null && !scopes.isBlank() ? scopes : "products:read,orders:write");
        key.setEnabled(true);
        storeApiKeyRepository.save(key);

        Map<String, Object> out = toPublic(key);
        out.put("apiKey", secret); // shown once
        return out;
    }

    @Transactional
    public void revoke(Long id) {
        Long fid = TenantContext.requireFournisseurId();
        StoreApiKey key = storeApiKeyRepository.findById(id)
                .filter(k -> fid.equals(k.getFournisseurId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clé introuvable"));
        key.setEnabled(false);
        key.setRevokedAt(LocalDateTime.now());
        storeApiKeyRepository.save(key);
    }

    @Transactional
    public StoreApiKey authenticate(String rawKey) {
        if (rawKey == null || rawKey.length() < 12) return null;
        String prefix = rawKey.substring(0, 12);
        StoreApiKey key = storeApiKeyRepository.findByKeyPrefixAndEnabledTrueAndRevokedAtIsNull(prefix)
                .orElse(null);
        if (key == null || !sha256(rawKey).equals(key.getKeyHash())) {
            return null;
        }
        key.setLastUsedAt(LocalDateTime.now());
        storeApiKeyRepository.save(key);
        return key;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private Map<String, Object> toPublic(StoreApiKey key) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", key.getId());
        m.put("name", key.getName());
        m.put("keyPrefix", key.getKeyPrefix());
        m.put("scopes", key.getScopes());
        m.put("enabled", key.isEnabled());
        m.put("lastUsedAt", key.getLastUsedAt());
        m.put("createdAt", key.getCreatedAt());
        m.put("revokedAt", key.getRevokedAt());
        return m;
    }
}
