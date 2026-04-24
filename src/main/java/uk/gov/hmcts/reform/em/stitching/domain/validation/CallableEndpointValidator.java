package uk.gov.hmcts.reform.em.stitching.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.URL;

public class CallableEndpointValidator implements ConstraintValidator<CallableEndpoint, String> {

    private final Logger log = LoggerFactory.getLogger(CallableEndpointValidator.class);

    private static final String PROD = "PROD";

    private final String allowedHost;
    private final String env;

    public CallableEndpointValidator(@Value("${callbackurlvalidator.host}") String host,
                                     @Value("${callbackurlvalidator.env}") String env) {
        this.allowedHost = host;
        this.env = env;
    }

    @Override
    public boolean isValid(String urlString, ConstraintValidatorContext context) {

        log.info("Validating callback urlString : {} for environment {} with host {}", urlString, env, allowedHost);
        if (StringUtils.isBlank(urlString)) {
            return false;
        }

        if (env.equalsIgnoreCase(PROD)) {
            URL url = null;
            try {
                url = new URI(urlString).toURL();
            } catch (Exception e) {
                log.error(String.format("Callback %s could not be verified", urlString), e);
                return false;
            }
            return url.getHost().equalsIgnoreCase(allowedHost);
        } else {
            return urlString.contains(allowedHost);
        }
    }
}
