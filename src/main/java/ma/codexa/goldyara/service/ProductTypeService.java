package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.entity.ProductType;
import ma.codexa.goldyara.repository.ProductTypeRepository;
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
public class ProductTypeService {

    private final ProductTypeRepository productTypeRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "productTypes")
    public List<ProductType> getAllProductTypes() {
        log.debug("Fetching all product types from database (cache miss)");
        return productTypeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ProductType> getProductTypeById(Long id) {
        return productTypeRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ProductType> getProductTypeByCode(String code) {
        return productTypeRepository.findByCode(code);
    }

    @CacheEvict(value = "productTypes", allEntries = true)
    public ProductType createProductType(ProductType productType) {
        log.info("Creating product type: {}", productType.getCode());
        return productTypeRepository.save(productType);
    }

    @CacheEvict(value = "productTypes", allEntries = true)
    public ProductType updateProductType(Long id, ProductType details) {
        ProductType productType = productTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type de produit", id));
        productType.setName(details.getName());
        productType.setCode(details.getCode());
        productType.setRequiresSize(details.getRequiresSize());
        productType.setSizeOptions(details.getSizeOptions());
        productType.setSortOrder(details.getSortOrder());
        log.info("Updated product type: {}", productType.getCode());
        return productTypeRepository.save(productType);
    }

    @CacheEvict(value = "productTypes", allEntries = true)
    public void deleteProductType(Long id) {
        ProductType productType = productTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type de produit", id));
        productTypeRepository.delete(productType);
        log.info("Deleted product type with id: {}", id);
    }
}
