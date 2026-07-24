package ma.codexa.troco.repository;

import ma.codexa.troco.entity.TopBarMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopBarMessageRepository extends JpaRepository<TopBarMessage, Long> {
    
    List<TopBarMessage> findByIsActiveOrderByDisplayOrderAsc(Boolean isActive);

    List<TopBarMessage> findByIsActiveTrueOrderByDisplayOrderAsc();

    /** Tous les messages (admin), tri par ordre d’affichage */
    List<TopBarMessage> findAllByOrderByDisplayOrderAsc();
}
