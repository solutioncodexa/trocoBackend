package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.entity.Collection;
import ma.codexa.goldyara.repository.CollectionRepository;
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
public class CollectionService {

    private final CollectionRepository collectionRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "collections")
    public List<Collection> getAllCollections() {
        log.debug("Fetching all collections from database (cache miss)");
        return collectionRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "collections", key = "'active'")
    public List<Collection> getActiveCollections() {
        log.debug("Fetching active collections from database (cache miss)");
        return collectionRepository.findAll().stream()
                .filter(Collection::getIsActive)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Collection> getCollectionById(Long id) {
        return collectionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Collection> getCollectionBySlug(String slug) {
        return collectionRepository.findBySlug(slug);
    }

    @CacheEvict(value = "collections", allEntries = true)
    public Collection createCollection(Collection collection) {
        log.info("Creating collection: {}", collection.getSlug());
        return collectionRepository.save(collection);
    }

    @CacheEvict(value = "collections", allEntries = true)
    public Collection updateCollection(Long id, Collection details) {
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", id));
        collection.setName(details.getName());
        collection.setSlug(details.getSlug());
        collection.setDescription(details.getDescription());
        collection.setIsActive(details.getIsActive());
        log.info("Updated collection: {}", collection.getSlug());
        return collectionRepository.save(collection);
    }

    @CacheEvict(value = "collections", allEntries = true)
    public void deleteCollection(Long id) {
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", id));
        collectionRepository.delete(collection);
        log.info("Deleted collection with id: {}", id);
    }
}
