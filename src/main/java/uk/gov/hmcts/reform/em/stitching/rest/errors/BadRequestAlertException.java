package uk.gov.hmcts.reform.em.stitching.rest.errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;

import java.net.URI;

public class BadRequestAlertException extends RuntimeException implements ErrorResponse {

    private static final long serialVersionUID = 1L;

    private final String entityName;
    private final String errorKey;
    private final ProblemDetail problemDetail;

    public BadRequestAlertException(String defaultMessage, String entityName, String errorKey) {
        this(ErrorConstants.DEFAULT_TYPE, defaultMessage, entityName, errorKey);
    }

    public BadRequestAlertException(URI type, String defaultMessage, String entityName, String errorKey) {
        super(defaultMessage);
        this.entityName = entityName;
        this.errorKey = errorKey;

        this.problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            defaultMessage
        );

        this.problemDetail.setType(type);

        this.problemDetail.setProperty("message", "error." + errorKey);
        this.problemDetail.setProperty("params", entityName);
    }

    public String getEntityName() {
        return entityName;
    }

    public String getErrorKey() {
        return errorKey;
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