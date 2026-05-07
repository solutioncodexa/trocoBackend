package ma.codexa.goldyara.repository;

import ma.codexa.goldyara.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("select c from Category c where c.showOnHero = true order by c.heroSortOrder asc nulls last, c.name asc")
    List<Category> findHeroCategoriesOrdered();
}
