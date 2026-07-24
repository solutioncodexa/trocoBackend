package ma.codexa.troco.repository;

import ma.codexa.troco.entity.StockSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockSettingsRepository extends JpaRepository<StockSettings, Long> {
}
