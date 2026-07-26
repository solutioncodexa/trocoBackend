package ma.codexa.troco.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Expose les fichiers du stockage LOCAL via {@code /uploads/**} (et la variante
 * {@code /api/uploads/**} due au context-path Spring).
 *
 * <p>En mode MinIO, ce mapping est désactivé : les fichiers sont servis
 * directement par MinIO/Nginx en URL absolue.</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = "file:" + uploadPath + "/";
        log.info("Mapping /uploads/** -> {}", location);

        registry.addResourceHandler("/uploads/**", "/api/uploads/**")
                .addResourceLocations(location, "classpath:/static/uploads/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
    }
}
