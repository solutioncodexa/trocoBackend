package ma.codexa.troco.repository;

import ma.codexa.troco.entity.InstagramConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Requêtes explicitement liées à la boutique : les appelants ne sont pas transactionnels (appels réseau). */
@Repository
public interface InstagramConnectionRepository extends JpaRepository<InstagramConnection, Long> {

    Optional<InstagramConnection> findByFournisseurId(Long fournisseurId);
}
