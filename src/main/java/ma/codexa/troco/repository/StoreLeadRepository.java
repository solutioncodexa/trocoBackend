package ma.codexa.troco.repository;

import ma.codexa.troco.entity.StoreLead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreLeadRepository extends JpaRepository<StoreLead, Long> {
    List<StoreLead> findAllByOrderByCreatedAtDesc();
}
