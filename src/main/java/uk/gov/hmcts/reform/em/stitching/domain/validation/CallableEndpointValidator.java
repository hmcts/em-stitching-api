package uk.gov.hmcts.reform.em.stitching.domain.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.net.HttpURLConnection;
import java.net.URL;

public class CallableEndpointValidator implements ConstraintValidator<CallableEndpoint, String> {

    private final Logger log = LoggerFactory.getLogger(CallableEndpointValidator.class);

    @Override
    public boolean isValid(String url, ConstraintValidatorContext context) {

        boolean valid;

        try {
            URL siteURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(3000);
            connection.connect();
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            valid = responseCode < 500;
        } catch (Exception e) {
            log.error(String.format("Callback %s could not be called", url), e);
            valid = false;
        }
        return valid;
    }

}
