package uk.gov.hmcts.reform.em.stitching.data.migration;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Flywayc implements FlywayConfigurationCustomizer {
    @Override
    public void customize(FluentConfiguration configuration) {
        configuration.configuration(Map.of("flyway.postgresql.transactional.lock","false"));
        configuration.envVars();
    }
}
