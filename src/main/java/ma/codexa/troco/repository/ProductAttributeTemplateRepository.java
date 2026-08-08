package ma.codexa.troco.repository;

import ma.codexa.troco.entity.ProductAttributeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductAttributeTemplateRepository extends JpaRepository<ProductAttributeTemplate, Long> {

    /** Modèle spécifique à une catégorie (filtré par tenant via Hibernate). */
    Optional<ProductAttributeTemplate> findByCategoryId(Long categoryId);

    /** Modèle par défaut de la boutique (category == null). */
    @Query("select t from ProductAttributeTemplate t where t.category is null")
    Optional<ProductAttributeTemplate> findBrandDefault();
}
