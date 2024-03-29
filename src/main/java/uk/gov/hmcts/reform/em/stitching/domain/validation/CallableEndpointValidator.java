package uk.gov.hmcts.reform.em.stitching.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;

import static uk.gov.hmcts.reform.em.stitching.service.CloseableCloser.close;

public class CallableEndpointValidator implements ConstraintValidator<CallableEndpoint, String> {

    private final Logger log = LoggerFactory.getLogger(CallableEndpointValidator.class);

    private final OkHttpClient okHttpClient;

    public CallableEndpointValidator(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    @Override
    public boolean isValid(String urlString, ConstraintValidatorContext context) {
        boolean valid;
        Response response = null;
        try {
            URL url = new URI(urlString).toURL();
            String urlWithoutPathString = String.format("%s://%s:%d",
                    url.getProtocol(),
                    url.getHost(),
                    url.getPort() < 0 ? url.getDefaultPort() : url.getPort());
            log.debug("Probing callback {}", urlWithoutPathString);
            URL urlWithoutPath = new URI(urlWithoutPathString).toURL();
            response = okHttpClient
                    .newCall(new Request.Builder()
                            .url(urlWithoutPath)
                            .build())
                    .execute();
            valid = response.code() < 500;
        } catch (Exception e) {
            log.error(String.format("Callback %s could not be verified", urlString), e);
            valid = false;
        } finally {
            close(response);
        }
        return valid;
    }

}
