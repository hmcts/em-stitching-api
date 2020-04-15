package uk.gov.hmcts.reform.em.stitching.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.idam.client.IdamApi;

@Configuration
@EnableFeignClients(basePackageClasses = {IdamApi.class})
public class Config {
}
