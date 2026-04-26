package ma.codexa.goldyara.repository;

import ma.codexa.goldyara.entity.PromoModal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromoModalRepository extends JpaRepository<PromoModal, Long> {
    
    List<PromoModal> findByIsActiveOrderByDisplayOrderAsc(Boolean isActive);
    
    Optional<PromoModal> findFirstByIsActiveOrderByDisplayOrderAsc(Boolean isActive);
    
    long countByIsActive(Boolean isActive);
}
