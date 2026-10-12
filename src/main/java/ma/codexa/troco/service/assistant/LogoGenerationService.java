package ma.codexa.troco.service.assistant;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.config.AssistantProperties;
import ma.codexa.troco.service.instagram.BytesMultipartFile;
import ma.codexa.troco.service.storage.StorageService;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Génération d'un logo par l'IA : consomme le quota quotidien d'images, stocke le logo (1024 px) et un favicon
 * (512 px) sans les appliquer à la boutique. Partagé par le parcours guidé du chat et l'outil de l'assistant.
 */
@Service
@RequiredArgsConstructor
public class LogoGenerationService {

    public record Result(String logoUrl, String faviconUrl) {
    }

    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");
    /** Le nom de marque finit dans une consigne au modèle : lettres, chiffres et ponctuation courante seulement. */
    private static final Pattern BRAND_CLEAN = Pattern.compile("[^\\p{L}\\p{N} &'.\\-]");

    private static final Map<String, String> SECTORS = Map.of(
            "jewelry", "a jewelry shop",
            "fashion", "a clothing and fashion shop",
            "beauty", "a beauty and cosmetics shop",
            "food", "a food and grocery shop",
            "craft", "a handmade crafts and decoration shop",
            "tech", "an electronics shop",
            "home", "a home and furniture shop");
    private static final Map<String, String> STYLES = Map.of(
            "minimal", "minimalist, clean lines",
            "modern", "modern and bold",
            "luxury", "elegant, premium and refined",
            "moroccan", "inspired by Moroccan patterns and zellige, warm and authentic",
            "playful", "friendly, colorful and playful");
    private static final Map<String, String> SYMBOLS = Map.of(
            "monogram", "built around the initial letter of the brand as a monogram",
            "icon", "with a simple symbol or icon above or beside the brand name",
            "text", "a wordmark: the brand name in distinctive lettering, no icon");
    private static final Map<String, String> COLORS = Map.of(
            "blue", "shades of blue",
            "green", "shades of green",
            "red", "red and dark red",
            "blackgold", "black and gold",
            "pink", "pink and rose",
            "purple", "purple and violet");

    private final LogoImageClient images;
    private final StorageService storage;
    private final AssistantProperties props;

    public int dailyLimit() {
        return images.dailyLimit();
    }

    /**
     * @param sector  code de secteur (facultatif)
     * @param style   code de style, ou texte libre court
     * @param symbol  code de symbole (facultatif)
     * @param colors  code de palette, ou couleurs #RRGGBB séparées par une virgule, ou texte libre court
     */
    public Result generate(String brandName, String sector, String style, String symbol, String colors) {
        if (!props.enabled()) {
            throw new BusinessException("L'assistant n'est pas activé.", HttpStatus.SERVICE_UNAVAILABLE, "ASSISTANT_DISABLED");
        }
        String brand = BRAND_CLEAN.matcher(brandName == null ? "" : brandName).replaceAll("").trim();
        if (brand.length() < 2) {
            throw new BusinessException("Nom de marque requis (2 caractères minimum)", HttpStatus.BAD_REQUEST, "LOGO_BRAND");
        }
        if (brand.length() > 40) brand = brand.substring(0, 40).trim();

        Long fid = TenantContext.requireFournisseurId();
        if (!images.tryReserve(fid)) {
            throw new BusinessException("Limite atteinte : " + images.dailyLimit()
                    + " logos générés par jour. Réessayez demain.", HttpStatus.TOO_MANY_REQUESTS, "LOGO_QUOTA");
        }
        byte[] raw;
        try {
            raw = images.generate(prompt(brand, sector, style, symbol, colors));
        } catch (RuntimeException e) {
            images.release(fid);
            throw new BusinessException("Génération du logo indisponible pour le moment",
                    HttpStatus.SERVICE_UNAVAILABLE, "LOGO_UNAVAILABLE");
        }
        String logoUrl = storage.store(new BytesMultipartFile("logo.png", "image/png", toPng(raw, 1024)));
        String faviconUrl = storage.store(new BytesMultipartFile("favicon.png", "image/png", toPng(raw, 512)));
        return new Result(logoUrl, faviconUrl);
    }

    /** Supprime les fichiers d'un logo non retenu (ignore les erreurs de stockage). */
    public void discard(Result r) {
        try {
            storage.delete(r.logoUrl());
            storage.delete(r.faviconUrl());
        } catch (RuntimeException ignored) {
            // fichier orphelin sans conséquence
        }
    }

    static String prompt(String brand, String sector, String style, String symbol, String colors) {
        StringBuilder sb = new StringBuilder("Logo design for the brand \"").append(brand).append("\"");
        String sec = sector == null ? null : SECTORS.get(sector);
        if (sec != null) sb.append(", ").append(sec);
        sb.append(". ");
        sb.append("Style: ").append(describe(STYLES, style, "simple and modern")).append(". ");
        String sym = symbol == null ? null : SYMBOLS.get(symbol);
        if (sym != null) sb.append("Concept: ").append(sym).append(". ");
        String col = colorDescription(colors);
        if (col != null) sb.append("Colors: ").append(col).append(". ");
        sb.append("Flat vector style, centered on a plain white background, the only text is the brand name, ")
                .append("no mockup, no watermark.");
        return sb.toString();
    }

    private static String describe(Map<String, String> known, String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String hit = known.get(value.trim());
        if (hit != null) return hit;
        String free = BRAND_CLEAN.matcher(value).replaceAll("").trim();
        return free.isEmpty() ? fallback : free.substring(0, Math.min(free.length(), 50));
    }

    private static String colorDescription(String colors) {
        if (colors == null || colors.isBlank()) return null;
        String c = colors.trim();
        String known = COLORS.get(c);
        if (known != null) return known;
        StringBuilder hexes = new StringBuilder();
        for (String part : c.split("[,\\s]+")) {
            if (HEX.matcher(part).matches()) hexes.append(hexes.length() > 0 ? ", " : "").append(part);
        }
        if (hexes.length() > 0) return "the brand colors " + hexes;
        String free = BRAND_CLEAN.matcher(c).replaceAll("").trim();
        return free.isEmpty() ? null : free.substring(0, Math.min(free.length(), 40));
    }

    /** Recadre au carré (centré) et redimensionne ; sortie PNG. */
    static byte[] toPng(byte[] source, int size) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(source));
            if (src == null) throw new IOException("illisible");
            int side = Math.min(src.getWidth(), src.getHeight());
            BufferedImage square = src.getSubimage((src.getWidth() - side) / 2, (src.getHeight() - side) / 2, side, side);
            BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(square, 0, 0, size, size, null);
            g.dispose();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(out, "png", bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Image générée illisible", HttpStatus.BAD_GATEWAY, "LOGO_IMAGE");
        }
    }
}
