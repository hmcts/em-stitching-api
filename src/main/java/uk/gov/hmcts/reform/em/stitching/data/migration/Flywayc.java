package uk.gov.hmcts.reform.em.stitching.data.migration;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Flywayc implements FlywayConfigurationCustomizer {
    private final Logger log = LoggerFactory.getLogger(Flywayc.class);

    @Override
    public void customize(FluentConfiguration configuration) {
        log.info("setting flyway configs ");
        configuration.configuration(Map.of("flyway.postgresql.transactional.lock","false"));
        configuration.envVars();
    }
}
