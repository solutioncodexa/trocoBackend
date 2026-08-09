package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.common.exception.ResourceNotFoundException;
import ma.codexa.troco.dto.BulkCategoryResultDTO;
import ma.codexa.troco.dto.CategoryCardDTO;
import ma.codexa.troco.dto.CategoryDTO;
import ma.codexa.troco.dto.CategoryHeroDTO;
import ma.codexa.troco.dto.CategoryNavDTO;
import ma.codexa.troco.dto.request.CreateCategoryRequest;
import ma.codexa.troco.dto.request.HeroCategoryPatchRequest;
import ma.codexa.troco.dto.request.UpdateCategoryRequest;
import ma.codexa.troco.entity.Category;
import ma.codexa.troco.repository.CategoryRepository;
import ma.codexa.troco.repository.ProductRepository;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    @Cacheable(
            value = "categories",
            key = "T(ma.codexa.troco.tenant.TenantContext).getFournisseurId() ?: 'none'"
    )
    public List<Category> getAllCategories() {
        log.debug("Fetching all categories from database (cache miss)");
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategoryDtos() {
        Map<Long, Long> counts = loadProductCountsByCategory();
        return categoryRepository.findAll().stream()
                .peek(c -> {
                    if (c.getParent() != null) {
                        Hibernate.initialize(c.getParent());
                    }
                })
                .map(c -> toDto(c, counts))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    /** Footer / nav — sans description ni productCount (évite N+1). Actives seulement. */
    @Transactional(readOnly = true)
    public List<CategoryNavDTO> getNavCategoryDtos() {
        return categoryRepository.findAll().stream()
                .filter(this::isActive)
                .map(c -> new CategoryNavDTO(
                        c.getId(),
                        c.getName(),
                        c.getSlug(),
                        c.getParent() != null ? c.getParent().getId() : null
                ))
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
    }

    /** Cartes vitrine (home / page builder) — sans productCount. Actives seulement. */
    @Transactional(readOnly = true)
    public List<CategoryCardDTO> getCardCategoryDtos() {
        return categoryRepository.findAll().stream()
                .filter(this::isActive)
                .map(c -> new CategoryCardDTO(
                        c.getId(),
                        c.getName(),
                        c.getSlug(),
                        c.getParent() != null ? c.getParent().getId() : null,
                        c.getHeroImageUrl()
                ))
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
    }

    /** Catégories affichées sur le bandeau d’accueil (ordre + image configurés en admin). */
    @Transactional(readOnly = true)
    @Cacheable(
            value = "heroCategories",
            key = "T(ma.codexa.troco.tenant.TenantContext).getFournisseurId() ?: 'none'"
    )
    public List<CategoryHeroDTO> getHeroCategoryDtos() {
        return categoryRepository.findHeroCategoriesOrdered().stream()
                .map(c -> new CategoryHeroDTO(
                        c.getId(),
                        c.getName(),
                        c.getSlug(),
                        c.getHeroImageUrl(),
                        c.getHeroSortOrder()
                ))
                .toList();
    }

    /** @deprecated préférer {@link #getHeroCategoryDtos()} — ne pas mettre d’entités en cache. */
    @Transactional(readOnly = true)
    public List<Category> getHeroCategories() {
        return categoryRepository.findHeroCategoriesOrdered();
    }

    @Transactional(readOnly = true)
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<CategoryDTO> getCategoryDtoById(Long id) {
        return categoryRepository.findById(id).map(c -> {
            if (c.getParent() != null) {
                Hibernate.initialize(c.getParent());
            }
            return toDto(c, loadProductCountsByCategory());
        });
    }

    @Transactional(readOnly = true)
    public Optional<CategoryDTO> getCategoryDtoBySlug(String slug) {
        return categoryRepository.findBySlug(slug).map(c -> {
            if (c.getParent() != null) {
                Hibernate.initialize(c.getParent());
            }
            return toDto(c, loadProductCountsByCategory());
        });
    }

    @Transactional(readOnly = true)
    public Optional<Category> getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }

    @CacheEvict(cacheNames = {"categories", "heroCategories"}, allEntries = true)
    public CategoryDTO createCategory(CreateCategoryRequest request) {
        String slug = normalizeSlug(request.getSlug());
        if (slug.isBlank()) {
            throw new BusinessException("Le slug est obligatoire", HttpStatus.BAD_REQUEST);
        }
        if (categoryRepository.existsBySlug(slug)) {
            throw new BusinessException("Une catégorie avec ce slug existe déjà", HttpStatus.CONFLICT);
        }
        Category category = new Category();
        category.setFournisseurId(ma.codexa.troco.tenant.TenantContext.getFournisseurId());
        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setDescription(blankToNull(request.getDescription()));
        category.setParent(resolveParent(request.getParentId(), null));
        category.setShowOnHero(false);
        category.setActive(true);
        Category saved = categoryRepository.save(category);
        log.info("Creating category: {}", saved.getSlug());
        return toDto(saved);
    }

    /** Legacy entity create (import). */
    @CacheEvict(cacheNames = {"categories", "heroCategories"}, allEntries = true)
    public Category createCategory(Category category) {
        if (category.getSlug() != null) {
            category.setSlug(normalizeSlug(category.getSlug()));
        }
        log.info("Creating category: {}", category.getSlug());
        return categoryRepository.save(category);
    }

    @CacheEvict(cacheNames = {"categories", "heroCategories"}, allEntries = true)
    public CategoryDTO updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));

        String slug = normalizeSlug(request.getSlug());
        if (slug.isBlank()) {
            throw new BusinessException("Le slug est obligatoire", HttpStatus.BAD_REQUEST);
        }
        categoryRepository.findBySlug(slug).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("Une catégorie avec ce slug existe déjà", HttpStatus.CONFLICT);
            }
        });

        category.setName(request.getName().trim());
        category.setDescription(blankToNull(request.getDescription()));
        category.setSlug(slug);

        if (Boolean.TRUE.equals(request.getClearParent())) {
            category.setParent(null);
        } else if (request.getParentId() != null) {
            category.setParent(resolveParent(request.getParentId(), id));
        }

        log.info("Updated category: {}", category.getSlug());
        return toDto(categoryRepository.save(category));
    }

    @CacheEvict(cacheNames = {"categories", "heroCategories"}, allEntries = true)
    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));

        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        category.setSlug(categoryDetails.getSlug() != null ? normalizeSlug(categoryDetails.getSlug()) : category.getSlug());

        log.info("Updated category: {}", category.getSlug());
        return categoryRepository.save(category);
    }

    @CacheEvict(cacheNames = {"categories", "heroCategories"}, allEntries = true)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));
        long products = productRepository.countByCategoryIdAndDeletedFalse(id);
        if (products > 0) {
            throw new BusinessException(
                    "Impossible de supprimer : " + products + " produit(s) utilisent cette catégorie",
                    HttpStatus.CONFLICT);
        }
        long children = categoryRepository.findAll().stream()
                .filter(c -> c.getParent() != null && id.equals(c.getParent().getId()))
                .count();
        if (children > 0) {
            throw new BusinessException(
                    "Impossible de supprimer : cette catégorie a des sous-catégories",
                    HttpStatus.CONFLICT);
        }
        categoryRepository.delete(category);
        log.info("Deleted category with id: {}", id);
    }

    /**
     * Suppression en masse : enfants d’abord, puis parents.
     * Continue sur les autres en cas d’échec partiel.
     */
    @CacheEvict(cacheNames = {"categories", "heroCategories"}, allEntries = true)
    public BulkCategoryResultDTO deleteCategories(List<Long> ids) {
        List<Long> ordered = orderForDeletion(ids);
        int ok = 0;
        List<String> errors = new java.util.ArrayList<>();
        for (Long id : ordered) {
            try {
                deleteCategory(id);
                ok += 1;
            } catch (BusinessException ex) {
                errors.add(ex.getMessage());
            } catch (Exception ex) {
                errors.add("Catégorie " + id + " : " + ex.getMessage());
            }
        }
        return BulkCategoryResultDTO.builder()
                .successCount(ok)
                .failureCount(errors.size())
                .errors(errors)
                .build();
    }

    @CacheEvict(cacheNames = {"categories", "heroCategories"}, allEntries = true)
    public BulkCategoryResultDTO setCategoriesActive(List<Long> ids, boolean active) {
        int ok = 0;
        List<String> errors = new java.util.ArrayList<>();
        for (Long id : ids) {
            if (id == null) continue;
            try {
                Category category = categoryRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));
                category.setActive(active);
                if (!active) {
                    category.setShowOnHero(false);
                }
                categoryRepository.save(category);
                ok += 1;
            } catch (ResourceNotFoundException ex) {
                errors.add(ex.getMessage());
            } catch (Exception ex) {
                errors.add("Catégorie " + id + " : " + ex.getMessage());
            }
        }
        return BulkCategoryResultDTO.builder()
                .successCount(ok)
                .failureCount(errors.size())
                .errors(errors)
                .build();
    }

    @CacheEvict(cacheNames = {"categories", "heroCategories"}, allEntries = true)
    public CategoryDTO setCategoryActive(Long id, boolean active) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));
        category.setActive(active);
        if (!active) {
            category.setShowOnHero(false);
        }
        Category saved = categoryRepository.save(category);
        if (saved.getParent() != null) {
            Hibernate.initialize(saved.getParent());
        }
        return toDto(saved);
    }

    @CacheEvict(cacheNames = {"categories", "heroCategories"}, allEntries = true)
    public CategoryDTO patchHeroCategory(Long id, HeroCategoryPatchRequest patch) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));
        if (patch.getShowOnHero() != null) {
            category.setShowOnHero(patch.getShowOnHero());
        }
        if (Boolean.TRUE.equals(patch.getAutomaticHeroSortOrder())) {
            category.setHeroSortOrder(null);
        } else if (patch.getHeroSortOrder() != null) {
            int o = patch.getHeroSortOrder();
            if (o < 0 || o > 9999) {
                throw new BusinessException(
                        "L'ordre doit être entre 0 et 9999, ou vide pour automatique.",
                        HttpStatus.BAD_REQUEST);
            }
            category.setHeroSortOrder(o);
        }
        if (patch.getHeroImageUrl() != null) {
            String url = patch.getHeroImageUrl().trim();
            if (url.length() > 1024) {
                throw new BusinessException("URL d'image trop longue (max 1024)", HttpStatus.BAD_REQUEST);
            }
            category.setHeroImageUrl(url.isEmpty() ? null : url);
        }
        Category saved = categoryRepository.save(category);
        log.info("Hero settings updated for category id={}", id);
        // Initialiser le parent tant que la session est ouverte
        if (saved.getParent() != null) {
            Hibernate.initialize(saved.getParent());
        }
        return toDto(saved);
    }

    public CategoryDTO toDto(Category category) {
        long productCount = category.getId() != null
                ? productRepository.countByCategoryIdAndDeletedFalse(category.getId())
                : 0;
        return toDto(category, Map.of(
                category.getId() != null ? category.getId() : -1L,
                productCount));
    }

    private CategoryDTO toDto(Category category, Map<Long, Long> countsByCategoryId) {
        Long parentId = null;
        String parentName = null;
        Category parent = category.getParent();
        if (parent != null) {
            parentId = parent.getId();
            if (Hibernate.isInitialized(parent)) {
                parentName = parent.getName();
            }
        }
        long productCount = category.getId() != null
                ? countsByCategoryId.getOrDefault(category.getId(), 0L)
                : 0L;
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentId(parentId)
                .parentName(parentName)
                .heroImageUrl(category.getHeroImageUrl())
                .showOnHero(Boolean.TRUE.equals(category.getShowOnHero()))
                .heroSortOrder(category.getHeroSortOrder())
                .productCount(productCount)
                .active(isActive(category))
                .build();
    }

    private boolean isActive(Category category) {
        return category.getActive() == null || Boolean.TRUE.equals(category.getActive());
    }

    /** Enfants avant parents pour permettre la suppression groupée d’une branche. */
    private List<Long> orderForDeletion(List<Long> ids) {
        java.util.Set<Long> wanted = new java.util.LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) wanted.add(id);
        }
        Map<Long, Category> byId = new HashMap<>();
        for (Category c : categoryRepository.findAll()) {
            byId.put(c.getId(), c);
        }
        return wanted.stream()
                .sorted((a, b) -> {
                    Category ca = byId.get(a);
                    Category cb = byId.get(b);
                    boolean aChild = ca != null && ca.getParent() != null;
                    boolean bChild = cb != null && cb.getParent() != null;
                    if (aChild != bChild) {
                        return aChild ? -1 : 1; // children first
                    }
                    return Long.compare(a, b);
                })
                .toList();
    }

    private Map<Long, Long> loadProductCountsByCategory() {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : productRepository.countActiveGroupedByCategoryId()) {
            if (row == null || row.length < 2 || row[0] == null) continue;
            Long catId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();
            map.put(catId, count);
        }
        return map;
    }

    private Category resolveParent(Long parentId, Long selfId) {
        if (parentId == null) {
            return null;
        }
        if (selfId != null && parentId.equals(selfId)) {
            throw new BusinessException("Une catégorie ne peut pas être sa propre parente", HttpStatus.BAD_REQUEST);
        }
        Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie parente", parentId));
        return parent;
    }

    private static String normalizeSlug(String slug) {
        if (slug == null) {
            return "";
        }
        return slug.trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9\\-]", "");
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
