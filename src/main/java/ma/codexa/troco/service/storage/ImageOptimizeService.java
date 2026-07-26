package ma.codexa.troco.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Redimensionne et convertit les images uploadées (WebP de préférence, sinon JPEG).
 * GIF / PDF / SVG passent inchangés.
 */
@Slf4j
@Service
public class ImageOptimizeService {

    private static final Set<String> SKIP = Set.of(
            "image/gif", "application/pdf", "image/svg+xml"
    );

    @Value("${app.upload.optimize:true}")
    private boolean optimizeEnabled;

    @Value("${app.upload.max-edge:2000}")
    private int maxEdge;

    @Value("${app.upload.webp-quality:0.82}")
    private float webpQuality;

    public OptimizedImage optimize(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide");
        }
        String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!optimizeEnabled || SKIP.contains(ct) || !ct.startsWith("image/")) {
            return new OptimizedImage(file.getBytes(), ct.isBlank() ? "application/octet-stream" : ct,
                    LocalStorageService.resolveExtension(file.getOriginalFilename()), false);
        }

        byte[] original = file.getBytes();
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(original));
        if (src == null) {
            return new OptimizedImage(original, ct,
                    LocalStorageService.resolveExtension(file.getOriginalFilename()), false);
        }

        BufferedImage scaled = scaleDown(src, Math.max(800, maxEdge));
        boolean hasAlpha = scaled.getColorModel().hasAlpha();

        // Prefer WebP when writer available
        if (ImageIO.getImageWritersByFormatName("webp").hasNext() && !hasAlpha) {
            byte[] webp = writeWebp(scaled, webpQuality);
            if (webp != null && webp.length > 0 && webp.length < original.length) {
                log.debug("image_optimized format=webp in={} out={}", original.length, webp.length);
                return new OptimizedImage(webp, "image/webp", ".webp", true);
            }
        }

        if (hasAlpha) {
            byte[] png = writePng(scaled);
            if (png != null && (png.length < original.length || !"image/png".equals(ct))) {
                return new OptimizedImage(png, "image/png", ".png", true);
            }
            return new OptimizedImage(original, ct, ".png", false);
        }

        byte[] jpeg = writeJpeg(scaled, 0.85f);
        if (jpeg != null && jpeg.length > 0 && jpeg.length < original.length) {
            log.debug("image_optimized format=jpeg in={} out={}", original.length, jpeg.length);
            return new OptimizedImage(jpeg, "image/jpeg", ".jpg", true);
        }

        // Already WebP and small enough — keep
        if ("image/webp".equals(ct)) {
            return new OptimizedImage(original, ct, ".webp", false);
        }
        return new OptimizedImage(original, ct, LocalStorageService.resolveExtension(file.getOriginalFilename()), false);
    }

    private static BufferedImage scaleDown(BufferedImage src, int maxEdge) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxEdge && h <= maxEdge) {
            return ensureRgb(src);
        }
        double scale = Math.min((double) maxEdge / w, (double) maxEdge / h);
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        int type = src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage out = new BufferedImage(nw, nh, type);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (type == BufferedImage.TYPE_INT_RGB) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, nw, nh);
        }
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private static BufferedImage ensureRgb(BufferedImage src) {
        if (!src.getColorModel().hasAlpha() && src.getType() == BufferedImage.TYPE_INT_RGB) return src;
        if (src.getColorModel().hasAlpha()) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    private static byte[] writeWebp(BufferedImage img, float quality) {
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
            if (!writers.hasNext()) return null;
            ImageWriter writer = writers.next();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    try {
                        param.setCompressionType("Lossy");
                    } catch (Exception ignored) { /* default */ }
                    param.setCompressionQuality(Math.max(0.5f, Math.min(quality, 0.95f)));
                }
                writer.write(null, new IIOImage(img, null, null), param);
            } finally {
                writer.dispose();
            }
            return baos.toByteArray();
        } catch (Exception e) {
            log.debug("webp_encode_failed: {}", e.getMessage());
            return null;
        }
    }

    private static byte[] writeJpeg(BufferedImage img, float quality) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                ImageIO.write(img, "jpg", baos);
                return baos.toByteArray();
            }
            ImageWriter writer = writers.next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(quality);
                }
                writer.write(null, new IIOImage(img, null, null), param);
            } finally {
                writer.dispose();
            }
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] writePng(BufferedImage img) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
