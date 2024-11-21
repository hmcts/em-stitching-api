package uk.gov.hmcts.reform.em.stitching.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Configuration
public class TestContainersConfig {
    private static final String POSTGRES_IMAGE = "hmctspublic.azurecr.io/imported/postgres:16-alpine";

    @Bean
    public PostgreSQLContainer<?> postgreSQLContainer() {
        DockerImageName customImage = DockerImageName.parse(POSTGRES_IMAGE)
                .asCompatibleSubstituteFor("postgres");

        PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(customImage)
                .withDatabaseName("emstitch")
                .withUsername("postgres")
                .withPassword("postgres");

        postgreSQLContainer.start();

        return postgreSQLContainer;
    }
}
