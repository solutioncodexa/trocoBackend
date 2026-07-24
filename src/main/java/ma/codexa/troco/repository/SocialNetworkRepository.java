package ma.codexa.troco.repository;

import ma.codexa.troco.entity.SocialNetwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocialNetworkRepository extends JpaRepository<SocialNetwork, Long> {

    List<SocialNetwork> findAllByOrderByDisplayOrderAsc();

    List<SocialNetwork> findByEnabledTrueOrderByDisplayOrderAsc();

    Optional<SocialNetwork> findByNetworkKey(String networkKey);
}
