package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.dto.ProductListItemDTO;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.mapper.ProductMapper;
import ma.codexa.troco.repository.OrderItemRepository;
import ma.codexa.troco.repository.ProductRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductUpsellService {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<ProductListItemDTO> frequentlyBoughtWith(Long productId, int limit) {
        Long fid = TenantContext.getFournisseurId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable"));

        int lim = Math.max(1, Math.min(limit, 12));
        Set<Long> ids = new LinkedHashSet<>();
        if (fid != null) {
            for (Object[] row : orderItemRepository.findFrequentlyBoughtWith(fid, productId, lim)) {
                if (row[0] != null) ids.add(((Number) row[0]).longValue());
            }
        }

        List<Product> result = new ArrayList<>();
        if (!ids.isEmpty()) {
            for (Product p : productRepository.findAllById(ids)) {
                if (!p.getId().equals(productId)) result.add(p);
            }
        }

        if (result.size() < lim && product.getCategory() != null) {
            Long catId = product.getCategory().getId();
            for (Product p : productRepository.findAll()) {
                if (result.size() >= lim) break;
                if (p.getId().equals(productId)) continue;
                if (p.getCategory() != null && catId.equals(p.getCategory().getId())
                        && result.stream().noneMatch(x -> x.getId().equals(p.getId()))) {
                    result.add(p);
                }
            }
        }

        return productMapper.toListItemDTOList(result.stream().limit(lim).toList());
    }
}
