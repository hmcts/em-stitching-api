package uk.gov.hmcts.reform.em.stitching.rest.errors;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;

import java.util.HashMap;
import java.util.Map;

import static uk.gov.hmcts.reform.em.stitching.rest.errors.ErrorConstants.DEFAULT_TYPE;

public class CustomParameterizedException extends RuntimeException implements ErrorResponse {

    private static final long serialVersionUID = 1L;
    private static final String PARAM = "param";

    private final ProblemDetail problemDetail;
    @Getter
    private final Map<String, Object> paramMap;


    public CustomParameterizedException(String message, String... params) {
        this(message, toParamMap(params));
    }

    public CustomParameterizedException(String message, Map<String, Object> paramMap) {
        super(message);

        this.paramMap = paramMap != null ? paramMap : new HashMap<>();

        this.problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Parameterized Exception"
        );

        this.problemDetail.setType(DEFAULT_TYPE);
        this.problemDetail.setProperty("message", message);
        this.problemDetail.setProperty("params", this.paramMap);
    }

    public static Map<String, Object> toParamMap(String... params) {
        Map<String, Object> map = new HashMap<>();
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                map.put(PARAM + i, params[i]);
            }
        }
        return map;
    }

    @Override
    public HttpStatusCode getStatusCode() {
        return HttpStatusCode.valueOf(problemDetail.getStatus());
    }

    @Override
    public ProblemDetail getBody() {
        return this.problemDetail;
    }
}