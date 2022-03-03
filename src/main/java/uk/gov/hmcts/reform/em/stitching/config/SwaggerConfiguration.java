package uk.gov.hmcts.reform.em.stitching.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("uk.gov.hmcts.reform.em.stitching.rest")
public class SwaggerConfiguration {

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(
                        new Info().title("EM Stitching API")
                                .description("API to stitch bundles into a PDF.\\n caseTypeId & jurisdictionId "
                                        + "are required attributes for Documents to use CDAM.")
                                .version("v0.1.0")
                );
    }
}
