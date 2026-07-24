package ma.codexa.troco.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.name}")
    private String appName;

    @Value("${app.version}")
    private String appVersion;

    @Value("${app.description}")
    private String appDescription;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(appName + " API")
                        .version(appVersion)
                        .description(appDescription)
                        .contact(new Contact()
                                .name("Troco Support")
                                .email("support@troco.ma"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://troco.ma")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("Serveur de développement"),
                        new Server()
                                .url("https://troco.ma/api")
                                .description("Serveur de production")
                ));
    }
}
