package ma.codexa.troco.repository;

import ma.codexa.troco.entity.StoreWebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreWebhookDeliveryRepository extends JpaRepository<StoreWebhookDelivery, Long> {
    List<StoreWebhookDelivery> findTop50ByOrderByCreatedAtDesc();
}
