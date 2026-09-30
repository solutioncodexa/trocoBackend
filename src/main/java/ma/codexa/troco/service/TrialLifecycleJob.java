package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cycle de vie de l'essai gratuit : rappel avant échéance, puis passage en PENDING à la fin. */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrialLifecycleJob {

    private final FournisseurService fournisseurService;

    @Scheduled(fixedDelayString = "${troco.trial.check-ms:600000}", initialDelayString = "${troco.trial.initial-delay-ms:60000}")
    public void run() {
        try {
            int reminded = fournisseurService.sendTrialReminders();
            if (reminded > 0) log.info("trial_reminders_sent count={}", reminded);
            int expired = fournisseurService.expireTrials();
            if (expired > 0) log.info("trials_expired count={}", expired);
        } catch (Exception e) {
            log.warn("trial_lifecycle_job_failed: {}", e.getMessage());
        }
    }
}
