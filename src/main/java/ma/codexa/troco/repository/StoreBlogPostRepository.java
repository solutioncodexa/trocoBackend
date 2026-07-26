package ma.codexa.troco.repository;

import ma.codexa.troco.entity.StoreBlogPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreBlogPostRepository extends JpaRepository<StoreBlogPost, Long> {
    List<StoreBlogPost> findAllByOrderByCreatedAtDesc();

    Optional<StoreBlogPost> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);

    List<StoreBlogPost> findByPublishedTrueOrderByCreatedAtDesc();
}
