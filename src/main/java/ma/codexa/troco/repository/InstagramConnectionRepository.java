package ma.codexa.troco.repository;

import ma.codexa.troco.entity.InstagramConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Requêtes explicitement liées à la boutique : les appelants ne sont pas transactionnels (appels réseau). */
@Repository
public interface InstagramConnectionRepository extends JpaRepository<InstagramConnection, Long> {

    Optional<InstagramConnection> findByFournisseurId(Long fournisseurId);

    /** Rappels de Meta (désautorisation, suppression) : sans session, la boutique se retrouve par le compte Instagram. */
    List<InstagramConnection> findByIgUserId(String igUserId);
}
