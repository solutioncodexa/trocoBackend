package ma.codexa.troco.repository;

import ma.codexa.troco.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Category> findByExternalWooId(Long externalWooId);

    @Query("select c from Category c where c.showOnHero = true order by c.heroSortOrder asc nulls last, c.name asc")
    List<Category> findHeroCategoriesOrdered();
}
