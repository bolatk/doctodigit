package ai.odoo.doctodigit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Properties are configured in the application.yml file.
 * </p>
 */

@Data
@Component
@ConfigurationProperties(prefix = "webservices.odoo")
public class OdooServiceProperties {
    private Auth auth;
    private String url;

    @Data
    public static class Auth {
        private String db;
        private String username;
        private String password;
    }
}
