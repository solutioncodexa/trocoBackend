package ma.codexa.troco.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.dto.HomeHeroSettingsDTO;
import ma.codexa.troco.entity.HomeHeroSettings;
import ma.codexa.troco.repository.HomeHeroSettingsRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeHeroSettingsService {

    public static final String DEFAULT_IMAGE_URL =
            "https://images.unsplash.com/photo-1616401784845-180882ba9ba8?w=2000&h=1400&fit=crop&q=85";

    public static final int MAX_IMAGES = 8;

    private final HomeHeroSettingsRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    @Cacheable(value = "homeHero")
    public HomeHeroSettingsDTO getPublic() {
        return toDto(getOrCreate());
    }

    @Transactional
    @CacheEvict(cacheNames = "homeHero", allEntries = true)
    public HomeHeroSettingsDTO update(HomeHeroSettingsDTO dto) {
        HomeHeroSettings settings = getOrCreate();
        List<String> urls = normalizeUrls(dto);
        settings.setImageUrlsJson(writeUrls(urls));
        settings.setImageUrl(urls.isEmpty() ? null : urls.get(0));
        HomeHeroSettings saved = repository.save(settings);
        log.info("Home hero images updated (count={})", urls.size());
        return toDto(saved);
    }

    private List<String> normalizeUrls(HomeHeroSettingsDTO dto) {
        List<String> raw = new ArrayList<>();
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            raw.addAll(dto.getImageUrls());
        } else if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            raw.add(dto.getImageUrl());
        }

        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String u : raw) {
            if (u == null) continue;
            String t = u.trim();
            if (!t.isEmpty()) {
                unique.add(t);
            }
            if (unique.size() >= MAX_IMAGES) break;
        }

        if (unique.isEmpty()) {
            unique.add(DEFAULT_IMAGE_URL);
        }
        return new ArrayList<>(unique);
    }

    private HomeHeroSettings getOrCreate() {
        return repository.findById(1L).orElseGet(() -> {
            HomeHeroSettings created = new HomeHeroSettings();
            created.setId(1L);
            created.setImageUrl(DEFAULT_IMAGE_URL);
            created.setImageUrlsJson(writeUrls(List.of(DEFAULT_IMAGE_URL)));
            return repository.save(created);
        });
    }

    private HomeHeroSettingsDTO toDto(HomeHeroSettings settings) {
        List<String> urls = readUrls(settings);
        if (urls.isEmpty()) {
            urls = List.of(DEFAULT_IMAGE_URL);
        }
        return HomeHeroSettingsDTO.builder()
                .imageUrl(urls.get(0))
                .imageUrls(urls)
                .build();
    }

    private List<String> readUrls(HomeHeroSettings settings) {
        List<String> fromJson = parseJson(settings.getImageUrlsJson());
        if (!fromJson.isEmpty()) {
            return fromJson;
        }
        if (settings.getImageUrl() != null && !settings.getImageUrl().isBlank()) {
            return List.of(settings.getImageUrl().trim());
        }
        return List.of();
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<>() {});
            if (list == null) return List.of();
            List<String> clean = new ArrayList<>();
            for (String u : list) {
                if (u != null && !u.isBlank()) {
                    clean.add(u.trim());
                }
            }
            return clean;
        } catch (Exception e) {
            log.warn("Invalid home hero image_urls JSON, falling back: {}", e.getMessage());
            return List.of();
        }
    }

    private String writeUrls(List<String> urls) {
        try {
            return objectMapper.writeValueAsString(urls);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize hero image urls", e);
        }
    }
}
