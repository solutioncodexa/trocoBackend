package ma.codexa.goldyara.repository;

import ma.codexa.goldyara.entity.GoldPriceSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoldPriceSettingRepository extends JpaRepository<GoldPriceSetting, Long> {

    Optional<GoldPriceSetting> findFirstByOrderByIdAsc();
}
