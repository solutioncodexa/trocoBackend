package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.dto.ProductReviewDTO;
import ma.codexa.troco.dto.ProductReviewSummaryDTO;
import ma.codexa.troco.dto.request.CreateProductReviewRequest;
import ma.codexa.troco.entity.ProductReview;
import ma.codexa.troco.repository.ProductRepository;
import ma.codexa.troco.repository.ProductReviewRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductReviewSummaryDTO publicSummary(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable");
        }
        List<ProductReviewDTO> reviews = reviewRepository
                .findByProductIdAndApprovedTrueOrderByCreatedAtDesc(productId)
                .stream().map(this::toDto).collect(Collectors.toList());
        double avg = reviews.stream().mapToInt(ProductReviewDTO::rating).average().orElse(0);
        return new ProductReviewSummaryDTO(productId, Math.round(avg * 10.0) / 10.0, reviews.size(), reviews);
    }

    @Transactional
    public ProductReviewDTO submit(CreateProductReviewRequest req) {
        Long fid = TenantContext.getFournisseurId();
        if (fid == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Boutique non résolue");
        }
        if (!productRepository.existsById(req.getProductId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable");
        }
        ProductReview r = new ProductReview();
        r.setFournisseurId(fid);
        r.setProductId(req.getProductId());
        r.setAuthorName(req.getAuthorName().trim());
        r.setAuthorEmail(blank(req.getAuthorEmail()));
        r.setRating(req.getRating());
        r.setTitle(blank(req.getTitle()));
        r.setBody(req.getBody().trim());
        r.setApproved(false);
        return toDto(reviewRepository.save(r));
    }

    @Transactional(readOnly = true)
    public List<ProductReviewDTO> listAdmin() {
        return reviewRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ProductReviewDTO setApproved(Long id, boolean approved) {
        ProductReview r = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avis introuvable"));
        r.setApproved(approved);
        return toDto(reviewRepository.save(r));
    }

    @Transactional
    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avis introuvable");
        }
        reviewRepository.deleteById(id);
    }

    private ProductReviewDTO toDto(ProductReview r) {
        return new ProductReviewDTO(
                r.getId(), r.getProductId(), r.getAuthorName(), r.getRating(),
                r.getTitle(), r.getBody(), Boolean.TRUE.equals(r.getApproved()), r.getCreatedAt());
    }

    private static String blank(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }
}
