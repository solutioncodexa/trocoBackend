package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AbandonedCartReminderJob {

    private final AbandonedCartService abandonedCartService;

    @Scheduled(fixedDelayString = "${troco.abandoned-cart.check-ms:120000}")
    public void run() {
        try {
            int n = abandonedCartService.processDueReminders();
            if (n > 0) log.info("abandoned_cart_reminders_sent count={}", n);
        } catch (Exception e) {
            log.warn("abandoned_cart_job_failed: {}", e.getMessage());
        }
    }
}
