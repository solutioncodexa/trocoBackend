package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Événements temps réel (STOMP) destinés à l'admin d'une boutique.
 * <p>
 * Le payload ne contient volontairement <b>aucun montant</b> : c'est un simple signal « les chiffres ont changé »,
 * le client recharge ensuite les statistiques via l'API authentifiée.
 * Le push est émis après commit pour ne jamais annoncer une modification annulée.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreEventPublisher {

    public static final String REVENUE_CHANGED = "REVENUE_CHANGED";

    private final SimpMessagingTemplate messagingTemplate;

    public static String revenueTopic(Long fournisseurId) {
        return "/topic/store." + fournisseurId + ".revenue";
    }

    /** Une commande a été créée / changée de statut / supprimée → les revenus et compteurs sont obsolètes. */
    public void publishRevenueChanged(Long fournisseurId, String reason, Long orderId, String orderStatus) {
        if (fournisseurId == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", REVENUE_CHANGED);
        payload.put("reason", reason);
        payload.put("orderId", orderId);
        payload.put("status", orderStatus);
        payload.put("at", Instant.now().toString());
        afterCommit(revenueTopic(fournisseurId), payload);
    }

    private void afterCommit(String destination, Object payload) {
        Runnable push = () -> {
            try {
                messagingTemplate.convertAndSend(destination, payload);
            } catch (Exception e) {
                log.warn("store_event_push_failed destination={} err={}", destination, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push.run();
                }
            });
        } else {
            push.run();
        }
    }
}
