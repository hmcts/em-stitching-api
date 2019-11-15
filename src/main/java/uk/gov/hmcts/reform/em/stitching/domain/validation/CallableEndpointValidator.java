package uk.gov.hmcts.reform.em.stitching.domain.validation;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CallableEndpointValidator implements ConstraintValidator<CallableEndpoint, String> {

    private final Logger log = LoggerFactory.getLogger(CallableEndpointValidator.class);

    private final OkHttpClient okHttpClient;

    public CallableEndpointValidator(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    @Override
    public boolean isValid(String url, ConstraintValidatorContext context) {

        boolean valid;

        try {
            Response response = okHttpClient
                    .newCall(new Request.Builder()
                            .method("POST", RequestBody.create("", MediaType.parse("text/plain")))
                            .url(url)
                            .build())
                    .execute();
            valid = response.code() < 500;
        } catch (Exception e) {
            log.error(String.format("Callback %s could not be verified", url), e);
            valid = false;
        }
        return valid;
    }

}
