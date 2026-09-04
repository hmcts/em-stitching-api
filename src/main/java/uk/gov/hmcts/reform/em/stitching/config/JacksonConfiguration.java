package uk.gov.hmcts.reform.em.stitching.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.datatype.hibernate7.Hibernate7Module;

@Configuration
public class JacksonConfiguration {

    /*
     * Support for Hibernate types in Jackson.
     */
    @Bean
    public Hibernate7Module hibernate7Module() {
        return new Hibernate7Module();
    }


}
