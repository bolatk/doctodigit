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
@ConfigurationProperties(prefix = "document")
public class FileProperties {

    private String inputFilePath;
    private String outputFilePath;
}
