package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.ResourceNotFoundException;
import ma.codexa.troco.dto.SocialNetworkDTO;
import ma.codexa.troco.entity.SocialNetwork;
import ma.codexa.troco.repository.SocialNetworkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialNetworkService {

    private final SocialNetworkRepository socialNetworkRepository;

    @Transactional
    public List<SocialNetworkDTO> getPublicEnabled() {
        ensureDefaults();
        return socialNetworkRepository.findByEnabledTrueOrderByDisplayOrderAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public List<SocialNetworkDTO> getAll() {
        ensureDefaults();
        return socialNetworkRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public List<SocialNetworkDTO> updateBatch(List<SocialNetworkDTO> updates) {
        ensureDefaults();
        if (updates == null || updates.isEmpty()) {
            return getAll();
        }
        for (SocialNetworkDTO dto : updates) {
            if (dto.getNetworkKey() == null || dto.getNetworkKey().isBlank()) {
                continue;
            }
            SocialNetwork entity = socialNetworkRepository.findByNetworkKey(dto.getNetworkKey().trim().toLowerCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Réseau social", dto.getNetworkKey()));
            if (dto.getUrl() != null && !dto.getUrl().isBlank()) {
                entity.setUrl(dto.getUrl().trim());
            }
            if (dto.getEnabled() != null) {
                entity.setEnabled(dto.getEnabled());
            }
            if (dto.getDisplayOrder() != null) {
                entity.setDisplayOrder(dto.getDisplayOrder());
            }
            if (dto.getLabel() != null && !dto.getLabel().isBlank()) {
                entity.setLabel(dto.getLabel().trim());
            }
            socialNetworkRepository.save(entity);
            log.info("Social network updated key={} enabled={} url={}",
                    entity.getNetworkKey(), entity.getEnabled(), entity.getUrl());
        }
        return socialNetworkRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Seed les 4 réseaux si la table est vide (ex. table créée par Hibernate
     * avant que Flyway V10 n'insère les lignes).
     */
    private void ensureDefaults() {
        if (socialNetworkRepository.count() > 0) {
            return;
        }
        log.warn("social_networks vide — seed des 4 réseaux par défaut");
        socialNetworkRepository.saveAll(List.of(
                network("facebook", "Facebook", "https://www.facebook.com/profile.php?id=61589818832364", 1),
                network("instagram", "Instagram", "https://www.instagram.com/troco/", 2),
                network("tiktok", "TikTok", "https://www.tiktok.com/@troco1", 3),
                network("whatsapp", "WhatsApp", "https://wa.me/212684490098", 4)
        ));
    }

    private static SocialNetwork network(String key, String label, String url, int order) {
        return SocialNetwork.builder()
                .networkKey(key)
                .label(label)
                .url(url)
                .enabled(true)
                .displayOrder(order)
                .build();
    }

    private SocialNetworkDTO toDto(SocialNetwork e) {
        return SocialNetworkDTO.builder()
                .id(e.getId())
                .networkKey(e.getNetworkKey())
                .label(e.getLabel())
                .url(e.getUrl())
                .enabled(e.getEnabled())
                .displayOrder(e.getDisplayOrder())
                .build();
    }
}
