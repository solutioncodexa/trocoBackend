package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.entity.StorePage;
import ma.codexa.troco.repository.StorePageRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Détecte les pages planifiées qui passent en ligne / hors ligne et notifie l’admin boutique.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StorePageScheduleNotifier {

    private final StorePageRepository pageRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "${troco.page-schedule.check-ms:60000}")
    @Transactional
    public void checkScheduledPages() {
        TenantContext.setBypass(true);
        try {
            List<StorePage> pages = pageRepository.findAll();
            LocalDateTime now = LocalDateTime.now();
            for (StorePage page : pages) {
                boolean live = isCurrentlyLive(page, now);
                Boolean last = page.getLastLiveState();
                if (last == null) {
                    page.setLastLiveState(live);
                    continue;
                }
                if (last == live) continue;

                page.setLastLiveState(live);
                Long fid = page.getFournisseurId();
                if (fid == null) continue;

                TenantContext.setFournisseurId(fid);
                try {
                    notificationService.notifyPageScheduleChange(
                            page.getId(),
                            page.getTitle(),
                            page.getSlug(),
                            live);
                } finally {
                    TenantContext.setFournisseurId(null);
                }
            }
        } catch (Exception e) {
            log.warn("page_schedule_check_failed: {}", e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    private static boolean isCurrentlyLive(StorePage page, LocalDateTime now) {
        if (!Boolean.TRUE.equals(page.getPublished())) return false;
        if (page.getPublishAt() != null && page.getPublishAt().isAfter(now)) return false;
        if (page.getUnpublishAt() != null && !page.getUnpublishAt().isAfter(now)) return false;
        return true;
    }
}
