package ma.codexa.troco.service.assistant;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.config.AssistantProperties;
import ma.codexa.troco.entity.Plan;
import ma.codexa.troco.service.PlanEntitlementService;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Consommation IA de la boutique courante : aujourd'hui face aux limites du plan, et les 7 derniers jours. */
@Service
@RequiredArgsConstructor
public class AiUsageSummaryService {

    private static final int DAYS = 7;

    private final AiUsageService usage;
    private final AssistantProperties props;
    private final LogoImageClient logoImages;
    private final PlanEntitlementService planService;

    public long grantTopup(Long fournisseurId, long tokens) {
        return usage.grantTopup(fournisseurId, tokens);
    }

    public Map<String, Object> summary() {
        Long fid = TenantContext.requireFournisseurId();
        Plan plan = planService.currentPlan();
        String today = LocalDate.now().toString();

        Map<String, Map<String, Integer>> byDay = new LinkedHashMap<>();
        for (int i = 0; i < DAYS; i++) {
            byDay.put(LocalDate.now().minusDays(i).toString(), new LinkedHashMap<>());
        }
        for (Map<String, Object> row : usage.history(fid, DAYS)) {
            String day = String.valueOf(row.get("usage_day"));
            Map<String, Integer> m = byDay.get(day);
            if (m != null) m.put(String.valueOf(row.get("kind")), ((Number) row.get("used")).intValue());
        }

        Map<String, Integer> now = byDay.getOrDefault(today, Map.of());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("plan", plan.getName());
        out.put("messages", meter(now.getOrDefault(AiUsageService.CHAT, 0), props.dailyLimitFor(plan.getCode())));
        out.put("tokens", meter(now.getOrDefault(AiUsageService.TOKENS, 0), usage.tokenBudgetFor(plan.getCode())));
        out.put("images", meter(now.getOrDefault(AiUsageService.IMAGE, 0), logoImages.dailyLimit()));
        out.put("topupTokens", usage.topupBalance(fid));

        List<Map<String, Object>> days = new ArrayList<>();
        byDay.forEach((day, m) -> {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("day", day);
            d.put("messages", m.getOrDefault(AiUsageService.CHAT, 0));
            d.put("tokens", m.getOrDefault(AiUsageService.TOKENS, 0));
            d.put("images", m.getOrDefault(AiUsageService.IMAGE, 0));
            days.add(d);
        });
        out.put("days", days);
        return out;
    }

    private static Map<String, Object> meter(int used, int limit) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("used", used);
        m.put("limit", limit);
        return m;
    }
}
