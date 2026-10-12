package ma.codexa.troco.service.assistant;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Compteurs d'usage IA persistants (boutique × type × jour). Chaque appel est atomique et validé tout de suite
 * (transaction indépendante), pour que deux requêtes simultanées ne dépassent pas la limite.
 */
@Service
@RequiredArgsConstructor
public class AiUsageService {

    public static final String IMAGE = "IMAGE";
    public static final String CHAT = "CHAT";
    public static final String TOKENS = "TOKENS";

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private ma.codexa.troco.service.PlanEntitlementService planService;

    @org.springframework.beans.factory.annotation.Value("${app.assistant.token-budget-basic:50000}")
    private int tokenBudgetBasic = 50_000;
    @org.springframework.beans.factory.annotation.Value("${app.assistant.token-budget-pro:200000}")
    private int tokenBudgetPro = 200_000;
    @org.springframework.beans.factory.annotation.Value("${app.assistant.token-budget-business:600000}")
    private int tokenBudgetBusiness = 600_000;

    private final JdbcTemplate jdbc;

    /** Réserve une unité du jour ; false si {@code limit} est déjà atteinte. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryReserve(Long fournisseurId, String kind, int limit) {
        int rows = jdbc.update("""
                INSERT INTO ai_usage_daily (fournisseur_id, kind, usage_day, used) VALUES (?, ?, ?, 1)
                ON CONFLICT (fournisseur_id, kind, usage_day)
                DO UPDATE SET used = ai_usage_daily.used + 1 WHERE ai_usage_daily.used < ?
                """, fournisseurId, kind, LocalDate.now(), limit);
        return rows > 0;
    }

    /** Budget quotidien de tokens du plan (entrée + sortie du modèle). */
    public int tokenBudgetFor(String planCode) {
        String c = planCode == null ? "basic" : planCode.trim().toLowerCase();
        return switch (c) {
            case "pro" -> tokenBudgetPro;
            case "business" -> tokenBudgetBusiness;
            default -> tokenBudgetBasic;
        };
    }

    /** Tokens déjà consommés aujourd'hui par la boutique. */
    public long tokensUsed(Long fournisseurId) {
        Long n = jdbc.queryForObject("SELECT COALESCE(SUM(used), 0) FROM ai_usage_daily "
                + "WHERE fournisseur_id = ? AND kind = ? AND usage_day = ?",
                Long.class, fournisseurId, TOKENS, LocalDate.now());
        return n == null ? 0 : n;
    }

    /** Ajoute les tokens réellement facturés par le fournisseur pour un appel. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addTokens(Long fournisseurId, long tokens) {
        if (tokens <= 0) return;
        long before = tokensUsed(fournisseurId);
        jdbc.update("""
                INSERT INTO ai_usage_daily (fournisseur_id, kind, usage_day, used) VALUES (?, ?, ?, ?)
                ON CONFLICT (fournisseur_id, kind, usage_day)
                DO UPDATE SET used = ai_usage_daily.used + EXCLUDED.used
                """, fournisseurId, TOKENS, LocalDate.now(), (int) Math.min(tokens, Integer.MAX_VALUE));
        deductOverflow(fournisseurId, before, tokens);
    }

    /** Consommation par jour et par type sur les {@code days} derniers jours (aujourd'hui inclus). */
    public java.util.List<java.util.Map<String, Object>> history(Long fournisseurId, int days) {
        return jdbc.queryForList("SELECT usage_day, kind, used FROM ai_usage_daily "
                + "WHERE fournisseur_id = ? AND usage_day > ? ORDER BY usage_day DESC, kind",
                fournisseurId, LocalDate.now().minusDays(days));
    }

    /** Solde de tokens supplémentaires de la boutique. */
    public long topupBalance(Long fournisseurId) {
        java.util.List<Long> r = jdbc.queryForList("SELECT balance FROM ai_token_topup WHERE fournisseur_id = ?",
                Long.class, fournisseurId);
        return r.isEmpty() ? 0 : r.get(0);
    }

    /** Ajoute (ou retire, si négatif) des tokens supplémentaires ; le solde ne descend pas sous zéro. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long grantTopup(Long fournisseurId, long tokens) {
        jdbc.update("""
                INSERT INTO ai_token_topup (fournisseur_id, balance) VALUES (?, GREATEST(?, 0))
                ON CONFLICT (fournisseur_id)
                DO UPDATE SET balance = GREATEST(ai_token_topup.balance + ?, 0), updated_at = CURRENT_TIMESTAMP
                """, fournisseurId, tokens, tokens);
        return topupBalance(fournisseurId);
    }

    /** La part d'un appel qui dépasse le budget du plan est prélevée sur le solde supplémentaire. */
    private void deductOverflow(Long fournisseurId, long before, long tokens) {
        if (planService == null) return;
        try {
            long budget = tokenBudgetFor(planService.currentPlan().getCode());
            long overflow = before + tokens - Math.max(budget, before);
            if (overflow > 0) {
                jdbc.update("UPDATE ai_token_topup SET balance = GREATEST(balance - ?, 0) WHERE fournisseur_id = ?",
                        overflow, fournisseurId);
            }
        } catch (RuntimeException ignored) {
            // le comptage principal est déjà enregistré ; le solde sera au pire légèrement généreux
        }
    }

    /** Rend une unité (échec côté fournisseur). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(Long fournisseurId, String kind) {
        jdbc.update("UPDATE ai_usage_daily SET used = GREATEST(used - 1, 0) "
                + "WHERE fournisseur_id = ? AND kind = ? AND usage_day = ?", fournisseurId, kind, LocalDate.now());
    }
}
