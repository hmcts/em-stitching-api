package uk.gov.hmcts.reform.em.stitching.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CallableEndpointValidator implements ConstraintValidator<CallableEndpoint, String> {

    private final Logger log = LoggerFactory.getLogger(CallableEndpointValidator.class);

    private static final String PROD = "PROD";

    private final List<String> allowedHosts;
    private final String env;

    public CallableEndpointValidator(@Value("${callbackurlvalidator.hosts:}") String hosts,
                                     @Value("${callbackurlvalidator.env:}") String env) {
        if (StringUtils.isBlank(hosts)) {
            this.allowedHosts = List.of();
        } else {
            this.allowedHosts = Arrays.stream(hosts.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        }
        this.env = env;
    }

    @Override
    public boolean isValid(String urlString, ConstraintValidatorContext context) {

        log.info("Validating callback urlString : {} for environment {} with allowedHosts {}",
                urlString, env, allowedHosts);
        if (StringUtils.isBlank(urlString)) {
            log.warn("CallBack URL is blank; rejecting callback {}", urlString);
            return false;
        }

        if (allowedHosts.isEmpty()) {
            log.warn("No allowed hosts configured; rejecting callback {}", urlString);
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
            String host = url.getHost();
            return allowedHosts.stream().anyMatch(a -> host.equalsIgnoreCase(a));
        } else {

            String lower = urlString.toLowerCase();
            //For non-prod, we check if the url contains any of the allowed hosts and not for exact match
            return allowedHosts.stream().anyMatch(a -> lower.contains(a.toLowerCase()));
        }
    }
}
