package ma.codexa.troco.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Application Meta « Instagram Login » (scope instagram_business_basic). Inactive tant qu'une valeur manque. */
@ConfigurationProperties(prefix = "app.instagram.oauth")
public record InstagramOAuthProperties(String clientId, String clientSecret, String redirectUri, String tokenKey) {

    public boolean configured() {
        return notBlank(clientId) && notBlank(clientSecret) && notBlank(redirectUri) && notBlank(tokenKey);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
