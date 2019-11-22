package uk.gov.hmcts.reform.em.stitching.domain.validation;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.net.URL;

public class CallableEndpointValidator implements ConstraintValidator<CallableEndpoint, String> {

    private final Logger log = LoggerFactory.getLogger(CallableEndpointValidator.class);

    private final OkHttpClient okHttpClient;

    public CallableEndpointValidator(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    @Override
    public boolean isValid(String urlString, ConstraintValidatorContext context) {

        boolean valid;

        try {
            URL url = new URL(urlString);
            String urlWithoutPathString = String.format("%s://%s:%d",
                    url.getProtocol(),
                    url.getHost(),
                    url.getPort() < 0 ? url.getDefaultPort() : url.getPort());
            log.info("Probing callback {}", urlWithoutPathString);
            URL urlWithoutPath = new URL(urlWithoutPathString);
            Response response = okHttpClient
                    .newCall(new Request.Builder()
                            .url(urlWithoutPath)
                            .build())
                    .execute();
            valid = response.code() < 500;
        } catch (Exception e) {
            log.error(String.format("Callback %s could not be verified", urlString), e);
            valid = false;
        }
        return valid;
    }

}
