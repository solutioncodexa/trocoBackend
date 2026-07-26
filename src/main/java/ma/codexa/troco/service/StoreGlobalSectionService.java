package ma.codexa.troco.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.dto.StoreGlobalSectionDTO;
import ma.codexa.troco.dto.request.UpsertGlobalSectionRequest;
import ma.codexa.troco.entity.StoreGlobalSection;
import ma.codexa.troco.repository.StoreGlobalSectionRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreGlobalSectionService {

    public static final Set<String> KEYS = Set.of("mega_menu", "footer_links", "sticky_cta", "app_bar");

    private final StoreGlobalSectionRepository repository;
    /** Boot 4 expose JsonMapper (Jackson 3), pas ObjectMapper — instance locale. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Lecture admin + seed des clés manquantes (écriture) — pas readOnly. */
    @Transactional
    public List<StoreGlobalSectionDTO> listAdmin() {
        ensureDefaults();
        return repository.findAllByOrderBySectionKeyAsc().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StoreGlobalSectionDTO> listPublicEnabled() {
        return repository.findByEnabledTrue().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public StoreGlobalSectionDTO upsert(UpsertGlobalSectionRequest req) {
        Long fid = TenantContext.requireFournisseurId();
        String key = normalizeKey(req.getSectionKey());
        StoreGlobalSection section = repository.findBySectionKey(key).orElseGet(() -> {
            StoreGlobalSection s = new StoreGlobalSection();
            s.setFournisseurId(fid);
            s.setSectionKey(key);
            s.setEnabled(false);
            s.setConfigJson(defaultConfig(key));
            return s;
        });
        if (req.getEnabled() != null) section.setEnabled(req.getEnabled());
        if (req.getConfig() != null) section.setConfigJson(writeJson(req.getConfig()));
        return toDto(repository.save(section));
    }

    private void ensureDefaults() {
        Long fid = TenantContext.requireFournisseurId();
        int created = 0;
        for (String key : KEYS) {
            if (repository.findBySectionKey(key).isEmpty()) {
                StoreGlobalSection s = new StoreGlobalSection();
                s.setFournisseurId(fid);
                s.setSectionKey(key);
                s.setEnabled(false);
                s.setConfigJson(defaultConfig(key));
                repository.save(s);
                created++;
            }
        }
        if (created > 0) {
            log.info("store_global_sections_seed fournisseurId={} created={} keys={}", fid, created, KEYS);
        }
    }

    private String defaultConfig(String key) {
        return switch (key) {
            case "mega_menu" -> writeJson(Map.of(
                    "items", List.of(
                            Map.of("label", "Boutique", "href", "/boutique", "children", List.of()),
                            Map.of("label", "Blog", "href", "/blog", "children", List.of())
                    )
            ));
            case "footer_links" -> writeJson(Map.of(
                    "columns", List.of(
                            Map.of("title", "Boutique", "links", List.of(
                                    Map.of("label", "Tous les produits", "href", "/boutique"),
                                    Map.of("label", "Blog", "href", "/blog")
                            )),
                            Map.of("title", "Aide", "links", List.of(
                                    Map.of("label", "Contact", "href", "/contact"),
                                    Map.of("label", "FAQ", "href", "/faq")
                            ))
                    )
            ));
            case "sticky_cta" -> writeJson(Map.of(
                    "text", "Besoin d’aide pour commander ?",
                    "ctaLabel", "Nous écrire",
                    "ctaHref", "/contact",
                    "dismissible", true
            ));
            case "app_bar" -> {
                Map<String, Object> cfg = new LinkedHashMap<>();
                cfg.put("bgColor", "");
                cfg.put("textColor", "");
                cfg.put("topBarEnabled", false);
                cfg.put("topBarText", "Livraison gratuite dès 500 DH");
                cfg.put("topBarBg", "#0F766E");
                cfg.put("topBarTextColor", "#FFFFFF");
                cfg.put("showSearch", true);
                cfg.put("showWishlist", true);
                cfg.put("showCart", true);
                cfg.put("navLabels", List.of("Boutique", "Sur-mesure", "Contact"));
                cfg.put("logoHeight", "md");
                cfg.put("sticky", true);
                yield writeJson(cfg);
            }
            default -> "{}";
        };
    }

    private String normalizeKey(String raw) {
        String key = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!KEYS.contains(key)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section inconnue: " + raw);
        }
        return key;
    }

    private StoreGlobalSectionDTO toDto(StoreGlobalSection s) {
        return new StoreGlobalSectionDTO(
                s.getId(),
                s.getSectionKey(),
                Boolean.TRUE.equals(s.getEnabled()),
                readJson(s.getConfigJson())
        );
    }

    private Map<String, Object> readJson(String json) {
        try {
            if (json == null || json.isBlank()) return new LinkedHashMap<>();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("global_section_config_invalid: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private String writeJson(Map<String, Object> config) {
        try {
            return objectMapper.writeValueAsString(config != null ? config : Map.of());
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configuration JSON invalide");
        }
    }
}
