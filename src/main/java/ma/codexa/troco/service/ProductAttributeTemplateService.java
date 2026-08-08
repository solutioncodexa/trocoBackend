package ma.codexa.troco.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.ResourceNotFoundException;
import ma.codexa.troco.dto.AttributeAxisDTO;
import ma.codexa.troco.dto.ProductAttributeTemplateDTO;
import ma.codexa.troco.dto.request.SaveAttributeTemplateRequest;
import ma.codexa.troco.entity.Category;
import ma.codexa.troco.entity.ProductAttributeTemplate;
import ma.codexa.troco.repository.CategoryRepository;
import ma.codexa.troco.repository.ProductAttributeTemplateRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gestion des modèles d'attributs de variantes (par catégorie ou par défaut boutique).
 * La résolution privilégie le modèle de catégorie, puis retombe sur celui de la boutique.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProductAttributeTemplateService {

    private final ProductAttributeTemplateRepository templateRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<ProductAttributeTemplateDTO> getAll() {
        return templateRepository.findAll().stream()
                .peek(t -> {
                    if (t.getCategory() != null) {
                        Hibernate.initialize(t.getCategory());
                    }
                })
                .map(this::toDto)
                .toList();
    }

    /**
     * Modèle à appliquer pour une catégorie donnée : modèle de catégorie sinon
     * modèle par défaut de la boutique sinon modèle vide.
     */
    @Transactional(readOnly = true)
    public ProductAttributeTemplateDTO resolve(Long categoryId) {
        if (categoryId != null) {
            Optional<ProductAttributeTemplate> byCategory = templateRepository.findByCategoryId(categoryId);
            if (byCategory.isPresent()) {
                return toDto(byCategory.get());
            }
        }
        return templateRepository.findBrandDefault()
                .map(this::toDto)
                .orElseGet(() -> ProductAttributeTemplateDTO.builder()
                        .categoryId(categoryId)
                        .axes(new ArrayList<>())
                        .build());
    }

    /** Crée ou met à jour le modèle de la catégorie (ou de la boutique si categoryId null). */
    public ProductAttributeTemplateDTO save(SaveAttributeTemplateRequest request) {
        Long categoryId = request.getCategoryId();
        Optional<ProductAttributeTemplate> existing = categoryId != null
                ? templateRepository.findByCategoryId(categoryId)
                : templateRepository.findBrandDefault();

        ProductAttributeTemplate template = existing.orElseGet(ProductAttributeTemplate::new);
        if (template.getFournisseurId() == null) {
            template.setFournisseurId(ma.codexa.troco.tenant.TenantContext.getFournisseurId());
        }

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Catégorie", categoryId));
            template.setCategory(category);
        } else {
            template.setCategory(null);
        }

        template.setAxesJson(writeAxes(sanitize(request.getAxes())));
        ProductAttributeTemplate saved = templateRepository.save(template);
        if (saved.getCategory() != null) {
            Hibernate.initialize(saved.getCategory());
        }
        log.info("attribute_template_saved id={} categoryId={}", saved.getId(), categoryId);
        return toDto(saved);
    }

    public void delete(Long id) {
        ProductAttributeTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modèle d'attributs", id));
        templateRepository.delete(template);
        log.info("attribute_template_deleted id={}", id);
    }

    private List<AttributeAxisDTO> sanitize(List<AttributeAxisDTO> axes) {
        if (axes == null) {
            return new ArrayList<>();
        }
        List<AttributeAxisDTO> cleaned = new ArrayList<>();
        for (AttributeAxisDTO axis : axes) {
            if (axis == null || axis.getName() == null || axis.getName().isBlank()) {
                continue;
            }
            List<String> values = new ArrayList<>();
            if (axis.getValues() != null) {
                for (String v : axis.getValues()) {
                    if (v != null && !v.isBlank()) {
                        values.add(v.trim());
                    }
                }
            }
            cleaned.add(new AttributeAxisDTO(axis.getName().trim(), values, axis.isRequired()));
        }
        return cleaned;
    }

    private String writeAxes(List<AttributeAxisDTO> axes) {
        try {
            return objectMapper.writeValueAsString(axes);
        } catch (Exception e) {
            log.warn("attribute_template_serialize_failed: {}", e.getMessage());
            return "[]";
        }
    }

    private List<AttributeAxisDTO> readAxes(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<AttributeAxisDTO> list = objectMapper.readValue(json, new TypeReference<List<AttributeAxisDTO>>() {});
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            log.warn("attribute_template_parse_failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private ProductAttributeTemplateDTO toDto(ProductAttributeTemplate template) {
        Category category = template.getCategory();
        Long categoryId = null;
        String categoryName = null;
        String categorySlug = null;
        if (category != null) {
            categoryId = category.getId();
            if (Hibernate.isInitialized(category)) {
                categoryName = category.getName();
                categorySlug = category.getSlug();
            }
        }
        return ProductAttributeTemplateDTO.builder()
                .id(template.getId())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .categorySlug(categorySlug)
                .axes(readAxes(template.getAxesJson()))
                .build();
    }
}
