package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.common.exception.ResourceNotFoundException;
import ma.codexa.troco.dto.CategoryDTO;
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

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "categories")
    public List<Category> getAllCategories() {
        log.debug("Fetching all categories from database (cache miss)");
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategoryDtos() {
        return categoryRepository.findAll().stream()
                .peek(c -> {
                    if (c.getParent() != null) {
                        Hibernate.initialize(c.getParent());
                    }
                })
                .map(this::toDto)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    /** Catégories affichées sur le bandeau d’accueil (ordre + image configurés en admin). */
    @Transactional(readOnly = true)
    @Cacheable(value = "heroCategories")
    public List<CategoryDTO> getHeroCategoryDtos() {
        return categoryRepository.findHeroCategoriesOrdered().stream()
                .peek(c -> {
                    if (c.getParent() != null) {
                        Hibernate.initialize(c.getParent());
                    }
                })
                .map(this::toDto)
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
            return toDto(c);
        });
    }

    @Transactional(readOnly = true)
    public Optional<CategoryDTO> getCategoryDtoBySlug(String slug) {
        return categoryRepository.findBySlug(slug).map(c -> {
            if (c.getParent() != null) {
                Hibernate.initialize(c.getParent());
            }
            return toDto(c);
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
        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setDescription(blankToNull(request.getDescription()));
        category.setParent(resolveParent(request.getParentId(), null));
        category.setShowOnHero(false);
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
                ? productRepository.countByCategoryIdAndDeletedFalse(category.getId())
                : 0;
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
                .build();
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
