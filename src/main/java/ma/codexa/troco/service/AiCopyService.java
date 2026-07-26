package ma.codexa.troco.service;

import ma.codexa.troco.dto.request.AiCopyRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Générateur de copy FR (templates + heuristiques).
 * Pas d’appel LLM externe — prêt à brancher une API plus tard.
 */
@Service
public class AiCopyService {

    public Map<String, Object> generate(AiCopyRequest req) {
        String kind = req.getKind().trim().toLowerCase(Locale.ROOT);
        String topic = req.getTopic().trim();
        String store = req.getStoreName() != null && !req.getStoreName().isBlank()
                ? req.getStoreName().trim() : "notre boutique";
        String tone = req.getTone() != null ? req.getTone().trim().toLowerCase(Locale.ROOT) : "friendly";

        return switch (kind) {
            case "seo_title" -> Map.of("text", clip(seoTitle(topic, store, tone), 60));
            case "seo_description" -> Map.of("text", clip(seoDescription(topic, store, tone), 155));
            case "hero" -> Map.of(
                    "headline", heroHeadline(topic, tone),
                    "subtext", heroSub(topic, store, tone),
                    "ctaLabel", tone.equals("luxury") ? "Découvrir" : "Voir la collection"
            );
            case "cta" -> Map.of(
                    "title", ctaTitle(topic, tone),
                    "body", "Réponse rapide, conseils personnalisés — " + store + ".",
                    "ctaLabel", "Nous contacter"
            );
            case "faq" -> Map.of("items", faqItems(topic, store));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de copy inconnu: " + kind);
        };
    }

    private static String seoTitle(String topic, String store, String tone) {
        if ("luxury".equals(tone)) return topic + " — " + store + " | Qualité premium";
        if ("pro".equals(tone)) return topic + " | " + store + " — Livraison Maroc";
        return topic + " pas cher au Maroc | " + store;
    }

    private static String seoDescription(String topic, String store, String tone) {
        if ("luxury".equals(tone)) {
            return "Découvrez " + topic + " chez " + store + ". Sélection soignée, finitions premium, livraison soignée partout au Maroc.";
        }
        return "Achetez " + topic + " chez " + store + ". Large choix, prix justes, livraison rapide au Maroc. Commandez en ligne en quelques clics.";
    }

    private static String heroHeadline(String topic, String tone) {
        if ("luxury".equals(tone)) return "L’élégance de " + topic;
        if ("pro".equals(tone)) return topic + " pour votre quotidien";
        return "Tout pour " + topic;
    }

    private static String heroSub(String topic, String store, String tone) {
        if ("luxury".equals(tone)) return "Une sélection exclusive par " + store + ".";
        return "Des pièces soigneusement choisies — " + store + ". Livraison partout au Maroc.";
    }

    private static String ctaTitle(String topic, String tone) {
        if ("luxury".equals(tone)) return "Une pièce sur-mesure ?";
        return "Une question sur " + topic + " ?";
    }

    private static List<Map<String, String>> faqItems(String topic, String store) {
        return List.of(
                map("Quels délais de livraison ?",
                        "24–48 h à Casablanca / Rabat, 2–4 jours ouvrés ailleurs au Maroc."),
                map("Puis-je retourner un article ?",
                        "Oui, sous 14 jours si l’article est intact. Contactez " + store + "."),
                map("Proposez-vous " + topic + " sur-mesure ?",
                        "Oui selon disponibilité — décrivez votre besoin via le formulaire de contact."),
                map("Quels moyens de paiement ?",
                        "Paiement à la livraison (COD) et options locales selon la boutique.")
        );
    }

    private static Map<String, String> map(String q, String a) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("q", q);
        m.put("a", a);
        return m;
    }

    private static String clip(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max - 1).trim() + "…";
    }
}
