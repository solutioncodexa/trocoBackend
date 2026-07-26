package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.ResourceNotFoundException;
import ma.codexa.troco.dto.SocialNetworkDTO;
import ma.codexa.troco.entity.SocialNetwork;
import ma.codexa.troco.repository.SocialNetworkRepository;
import ma.codexa.troco.tenant.TenantContext;
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
     * Seed neutre (désactivé, sans URL marque) pour une boutique vide —
     * chaque fournisseur configure ses propres réseaux.
     */
    private void ensureDefaults() {
        if (socialNetworkRepository.count() > 0) {
            return;
        }
        Long fid = TenantContext.getFournisseurId();
        log.info("social_networks vide — seed neutre fournisseurId={}", fid);
        socialNetworkRepository.saveAll(List.of(
                network(fid, "facebook", "Facebook", 1),
                network(fid, "instagram", "Instagram", 2),
                network(fid, "tiktok", "TikTok", 3),
                network(fid, "whatsapp", "WhatsApp", 4)
        ));
    }

    private static SocialNetwork network(Long fid, String key, String label, int order) {
        SocialNetwork n = SocialNetwork.builder()
                .networkKey(key)
                .label(label)
                .url("")
                .enabled(false)
                .displayOrder(order)
                .build();
        n.setFournisseurId(fid);
        return n;
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
