package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.dto.FeaturedProductDTO;
import ma.codexa.goldyara.dto.request.CreateFeaturedProductRequest;
import ma.codexa.goldyara.dto.request.ReorderFeaturedProductsRequest;
import ma.codexa.goldyara.dto.request.ToggleFeaturedProductRequest;
import ma.codexa.goldyara.dto.request.UpdateFeaturedProductRequest;
import ma.codexa.goldyara.entity.FeaturedProduct;
import ma.codexa.goldyara.entity.Product;
import ma.codexa.goldyara.mapper.FeaturedProductMapper;
import ma.codexa.goldyara.repository.FeaturedProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FeaturedProductService {

    private final FeaturedProductRepository featuredProductRepository;
    private final ProductService productService;
    private final FeaturedProductMapper featuredProductMapper;

    @Transactional(readOnly = true)
    public List<FeaturedProductDTO> getAllFeaturedProducts() {
        log.debug("Récupération de tous les produits sélectionnés");
        List<FeaturedProduct> featuredProducts = featuredProductRepository.findByIsActiveOrderByDisplayOrder(true);
        return featuredProducts.stream()
                .map(featuredProductMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeaturedProductDTO> getFeaturedProductsBySection(String section) {
        log.debug("Récupération des produits sélectionnés pour la section: {}", section);
        FeaturedProduct.Section sectionEnum = FeaturedProduct.Section.fromValue(section);
        List<FeaturedProduct> featuredProducts = featuredProductRepository
                .findBySectionAndIsActiveOrderByDisplayOrder(sectionEnum, true);
        return featuredProducts.stream()
                .map(featuredProductMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FeaturedProductDTO getFeaturedProductById(Long id) {
        log.debug("Récupération du produit sélectionné avec l'id: {}", id);
        FeaturedProduct featuredProduct = featuredProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit sélectionné", id));
        return featuredProductMapper.toDTO(featuredProduct);
    }

    public FeaturedProductDTO createFeaturedProduct(CreateFeaturedProductRequest request) {
        log.info("Création d'un nouveau produit sélectionné pour le produit ID: {}", request.getProductId());

        // Vérifier si le produit existe
        Product product = productService.getProductById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", request.getProductId()));

        // Vérifier si le produit est déjà dans cette section
        FeaturedProduct.Section section = FeaturedProduct.Section.fromValue(request.getSection());
        if (featuredProductRepository.existsByProductIdAndSection(request.getProductId(), section)) {
            throw new IllegalArgumentException("Ce produit est déjà sélectionné dans cette section");
        }

        FeaturedProduct featuredProduct = new FeaturedProduct();
        featuredProduct.setProduct(product);
        featuredProduct.setSection(section);
        featuredProduct.setTitle(request.getTitle());
        featuredProduct.setDescription(request.getDescription());
        featuredProduct.setImageUrl(request.getImageUrl());
        featuredProduct.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 
                getNextDisplayOrder(section));
        featuredProduct.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        FeaturedProduct savedFeaturedProduct = featuredProductRepository.save(featuredProduct);
        log.info("Produit sélectionné créé avec succès - ID: {}", savedFeaturedProduct.getId());
        return featuredProductMapper.toDTO(savedFeaturedProduct);
    }

    public FeaturedProductDTO updateFeaturedProduct(Long id, UpdateFeaturedProductRequest request) {
        log.info("Mise à jour du produit sélectionné avec l'id: {}", id);

        FeaturedProduct existingFeaturedProduct = featuredProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit sélectionné", id));

        // Mettre à jour les champs
        if (request.getProductId() != null && !request.getProductId().equals(existingFeaturedProduct.getProduct().getId())) {
            Product newProduct = productService.getProductById(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", request.getProductId()));
            
            // Vérifier si le nouveau produit est déjà dans cette section
            FeaturedProduct.Section section = request.getSection() != null ? 
                    FeaturedProduct.Section.fromValue(request.getSection()) : existingFeaturedProduct.getSection();
            // Ne vérifier que si le produit change vraiment
            if (!request.getProductId().equals(existingFeaturedProduct.getProduct().getId()) &&
                featuredProductRepository.existsByProductIdAndSection(request.getProductId(), section)) {
                throw new IllegalArgumentException("Ce produit est déjà sélectionné dans cette section");
            }
            
            existingFeaturedProduct.setProduct(newProduct);
        }

        if (request.getSection() != null) {
            FeaturedProduct.Section newSection = FeaturedProduct.Section.fromValue(request.getSection());
            
            // Vérifier si le produit est déjà dans la nouvelle section
            Long productId = request.getProductId() != null ? 
                    request.getProductId() : existingFeaturedProduct.getProduct().getId();
            // Ne vérifier que si la section change vraiment
            if (!newSection.equals(existingFeaturedProduct.getSection()) &&
                featuredProductRepository.existsByProductIdAndSection(productId, newSection)) {
                throw new IllegalArgumentException("Ce produit est déjà sélectionné dans cette section");
            }
            
            existingFeaturedProduct.setSection(newSection);
        }

        if (request.getTitle() != null) {
            existingFeaturedProduct.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            existingFeaturedProduct.setDescription(request.getDescription());
        }
        if (request.getImageUrl() != null) {
            existingFeaturedProduct.setImageUrl(request.getImageUrl());
        }
        if (request.getDisplayOrder() != null) {
            existingFeaturedProduct.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            existingFeaturedProduct.setIsActive(request.getIsActive());
        }

        FeaturedProduct savedFeaturedProduct = featuredProductRepository.save(existingFeaturedProduct);
        log.info("Produit sélectionné mis à jour avec succès - ID: {}", savedFeaturedProduct.getId());
        return featuredProductMapper.toDTO(savedFeaturedProduct);
    }

    public void deleteFeaturedProduct(Long id) {
        log.info("Suppression du produit sélectionné avec l'id: {}", id);
        
        FeaturedProduct featuredProduct = featuredProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit sélectionné", id));
        
        featuredProductRepository.delete(featuredProduct);
        log.info("Produit sélectionné supprimé avec succès - ID: {}", id);
    }

    public void reorderFeaturedProducts(ReorderFeaturedProductsRequest request) {
        log.info("Réorganisation des produits sélectionnés pour la section: {}", request.getSection());
        
        FeaturedProduct.Section section = FeaturedProduct.Section.fromValue(request.getSection());
        List<Long> productIds = request.getProductIds();
        
        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            FeaturedProduct featuredProduct = featuredProductRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produit sélectionné", productId));
            
            if (!featuredProduct.getSection().equals(section)) {
                throw new IllegalArgumentException("Le produit " + productId + " n'appartient pas à la section " + section);
            }
            
            featuredProduct.setDisplayOrder(i);
            featuredProductRepository.save(featuredProduct);
        }
        
        log.info("Produits sélectionnés réorganisés avec succès pour la section: {}", section);
    }

    public FeaturedProductDTO toggleFeaturedProduct(Long id, ToggleFeaturedProductRequest request) {
        log.info("Modification du statut du produit sélectionné avec l'id: {} - isActive: {}", id, request.getIsActive());
        
        FeaturedProduct featuredProduct = featuredProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit sélectionné", id));
        
        featuredProduct.setIsActive(request.getIsActive());
        FeaturedProduct savedFeaturedProduct = featuredProductRepository.save(featuredProduct);
        
        log.info("Statut du produit sélectionné modifié avec succès - ID: {}", id);
        return featuredProductMapper.toDTO(savedFeaturedProduct);
    }

    private Integer getNextDisplayOrder(FeaturedProduct.Section section) {
        Integer maxOrder = featuredProductRepository.findMaxDisplayOrderBySection(section);
        return maxOrder != null ? maxOrder + 1 : 0;
    }
}
