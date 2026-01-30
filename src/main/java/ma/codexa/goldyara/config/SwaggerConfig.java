package ma.codexa.goldyara.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("goldyara-api")
                .displayName("Goldyara API")
                .pathsToMatch("/**")
                .pathsToExclude("/actuator/**", "/error")
                .build();
    }
}
