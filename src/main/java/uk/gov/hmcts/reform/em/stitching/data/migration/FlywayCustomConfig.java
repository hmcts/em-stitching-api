package uk.gov.hmcts.reform.em.stitching.data.migration;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "FLYWAY_POSTGRESQL_TRANSACTIONAL_LOCK", havingValue = "false")
public class FlywayCustomConfig implements FlywayConfigurationCustomizer {
    @Override
    public void customize(FluentConfiguration configuration) {
        configuration.envVars();
    }
}