package ma.codexa.troco.repository;

import ma.codexa.troco.entity.StoreWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreWebhookRepository extends JpaRepository<StoreWebhook, Long> {
    List<StoreWebhook> findAllByOrderByCreatedAtDesc();

    List<StoreWebhook> findByEnabledTrue();
}
