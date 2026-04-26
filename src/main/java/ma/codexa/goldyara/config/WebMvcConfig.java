package ma.codexa.goldyara.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.UrlResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mapping pour les fichiers uploadés (externe) - accessible via /uploads/**
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = "file:" + uploadPath + "/";
        
        // Servir les fichiers uploadés directement sans préfixe /api
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
        
        // Mapping pour les ressources statiques (placeholder)
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations("classpath:/static/uploads/");
    }
}
