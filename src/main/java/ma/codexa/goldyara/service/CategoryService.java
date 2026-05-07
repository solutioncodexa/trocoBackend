package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.dto.request.HeroCategoryPatchRequest;
import ma.codexa.goldyara.entity.Category;
import ma.codexa.goldyara.repository.CategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "categories")
    public List<Category> getAllCategories() {
        log.debug("Fetching all categories from database (cache miss)");
        return categoryRepository.findAll();
    }

    /** Catégories affichées sur le bandeau d’accueil (ordre + image configurés en admin). */
    @Transactional(readOnly = true)
    @Cacheable(value = "heroCategories")
    public List<Category> getHeroCategories() {
        return categoryRepository.findHeroCategoriesOrdered();
    }

    @Transactional(readOnly = true)
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Category> getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }

    @CacheEvict(cacheNames = {"categories", "heroCategories"}, allEntries = true)
    public Category createCategory(Category category) {
        log.info("Creating category: {}", category.getSlug());
        return categoryRepository.save(category);
    }

    @CacheEvict(cacheNames = {"categories", "heroCategories"}, allEntries = true)
    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));

        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        category.setSlug(categoryDetails.getSlug());

        log.info("Updated category: {}", category.getSlug());
        return categoryRepository.save(category);
    }

    @CacheEvict(cacheNames = {"categories", "heroCategories"}, allEntries = true)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));
        categoryRepository.delete(category);
        log.info("Deleted category with id: {}", id);
    }

    @CacheEvict(cacheNames = {"categories", "heroCategories"}, allEntries = true)
    public Category patchHeroCategory(Long id, HeroCategoryPatchRequest patch) {
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
                throw new IllegalArgumentException("L'ordre doit être entre 0 et 9999, ou vide pour automatique.");
            }
            category.setHeroSortOrder(o);
        }
        if (patch.getHeroImageUrl() != null) {
            String url = patch.getHeroImageUrl().trim();
            category.setHeroImageUrl(url.isEmpty() ? null : url);
        }
        log.info("Hero settings updated for category id={}", id);
        return categoryRepository.save(category);
    }
}
