package uk.gov.hmcts.reform.em.stitching.rest.errors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.em.stitching.rest.util.HeaderUtil;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 * The error response follows RFC7807
 */
@RestControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {

    private static final String MESSAGE_FIELD = "message";

    private static final String FIELD_ERRORS = "fieldErrors";

    private final MessageSource messageSource;

    private final LocaleResolver localeResolver;

    public ExceptionTranslator(MessageSource messageSource, LocaleResolver localeResolver) {
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(
        ConstraintViolationException ex, WebRequest request) {

        List<FieldErrorVM> fieldErrors = ex.getConstraintViolations().stream()
            .map(violation -> {

                String fieldPath = violation.getPropertyPath().toString();
                String fieldName = fieldPath.contains(".")
                    ? fieldPath.substring(fieldPath.lastIndexOf('.') + 1)
                    : fieldPath;

                return new FieldErrorVM(
                    violation.getRootBeanClass().getSimpleName(),
                    fieldName,
                    violation.getMessage());
            })
            .toList();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "");
        problemDetail.setType(ErrorConstants.CONSTRAINT_VIOLATION_TYPE);
        problemDetail.setTitle("Constraint violation");
        problemDetail.setProperty(FIELD_ERRORS, fieldErrors);
        problemDetail.setProperty(MESSAGE_FIELD, ErrorConstants.ERR_VALIDATION);

        return new ResponseEntity<>(problemDetail, new HttpHeaders(), problemDetail.getStatus());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {

        BindingResult result = ex.getBindingResult();
        List<FieldErrorVM> fieldErrors = result.getFieldErrors().stream()
            .map(f -> new FieldErrorVM(f.getObjectName(), f.getField(),
                messageSource.getMessage(f.getCode(), null, f.getDefaultMessage(),
                    localeResolver.resolveLocale(
                        ((NativeWebRequest) request).getNativeRequest(HttpServletRequest.class)))))
            .toList();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, "");

        problemDetail.setType(ErrorConstants.CONSTRAINT_VIOLATION_TYPE);
        problemDetail.setTitle("Method argument not valid");
        problemDetail.setProperty(FIELD_ERRORS, fieldErrors);
        problemDetail.setProperty(MESSAGE_FIELD, ErrorConstants.ERR_VALIDATION);

        return new ResponseEntity<>(problemDetail, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
        HttpRequestMethodNotSupportedException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, "");

        problemDetail.setProperty(MESSAGE_FIELD, "error.http.405");
        problemDetail.setProperty("detail", ex.getMessage());

        return new ResponseEntity<>(problemDetail, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
        MissingServletRequestParameterException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {

        ProblemDetail problemDetail = ex.getBody();

        problemDetail.setProperty(MESSAGE_FIELD, ErrorConstants.BAD_REQUEST);

        return new ResponseEntity<>(problemDetail, new HttpHeaders(), problemDetail.getStatus());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(
        MissingServletRequestPartException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {

        ProblemDetail problemDetail = ex.getBody();

        problemDetail.setProperty(MESSAGE_FIELD, ErrorConstants.BAD_REQUEST);

        return new ResponseEntity<>(problemDetail, new HttpHeaders(), problemDetail.getStatus());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Object> handleNoSuchElementException(
        NoSuchElementException ex,
        NativeWebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "");
        problemDetail.setProperty(MESSAGE_FIELD, ErrorConstants.ENTITY_NOT_FOUND_TYPE);

        return new ResponseEntity<>(problemDetail, new HttpHeaders(), problemDetail.getStatus());
    }

    @ExceptionHandler(BadRequestAlertException.class)
    public ResponseEntity<Object> handleBadRequestAlertException(
        BadRequestAlertException ex,
        NativeWebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "");
        problemDetail.setProperty(MESSAGE_FIELD, "error." + ex.getErrorKey());

        HttpHeaders headers = HeaderUtil.createFailureAlert(ex.getEntityName(), ex.getMessage());
        return new ResponseEntity<>(problemDetail, headers, problemDetail.getStatus());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Object> handleBindException(
        BindException ex,
        NativeWebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "");
        problemDetail.setProperty(MESSAGE_FIELD, ErrorConstants.BAD_REQUEST);

        return new ResponseEntity<>(problemDetail, new HttpHeaders(), problemDetail.getStatus());
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<Object> handleConcurrencyFailure(
        ConcurrencyFailureException ex,
        NativeWebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "");
        problemDetail.setProperty(MESSAGE_FIELD, ErrorConstants.ERR_CONCURRENCY_FAILURE);

        return new ResponseEntity<>(problemDetail, new HttpHeaders(), problemDetail.getStatus());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(
        AccessDeniedException ex,
        NativeWebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "");
        problemDetail.setProperty(MESSAGE_FIELD, ErrorConstants.ERR_FORBIDDEN);

        return new ResponseEntity<>(problemDetail, new HttpHeaders(), problemDetail.getStatus());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleUnAuthorised(
        BadCredentialsException ex,
        NativeWebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "");
        problemDetail.setProperty(MESSAGE_FIELD, ErrorConstants.ERR_UNAUTHORISED);

        HttpServletRequest nativeRequest = request.getNativeRequest(HttpServletRequest.class);
        if (nativeRequest != null) {
            problemDetail.setProperty("path", nativeRequest.getRequestURI());
        }

        return new ResponseEntity<>(problemDetail, new HttpHeaders(), problemDetail.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedRuntime(Exception ex, WebRequest request) {

        // Dynamic look-up for library exceptions using @ResponseStatus
        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(
            ex.getClass(),
            ResponseStatus.class
        );

        // If a library exception HAS @ResponseStatus, extract and use its code!
        if (responseStatus != null) {
            HttpStatus libraryStatus = responseStatus.value();

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                libraryStatus,
                ex.getMessage()
            );
            if (StringUtils.isNotBlank(responseStatus.reason())) {
                problemDetail.setTitle(responseStatus.reason());
            }
            addCommonProperties(problemDetail, request);
            return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
        }

        // Default 500 fallback logic for any completely untagged RuntimeException
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected internal server error occurred.");
        detail.setTitle("Internal Server Error");

        addCommonProperties(detail, request);
        return ResponseEntity.status(detail.getStatus()).body(detail);
    }

    @ExceptionHandler(CustomParameterizedException.class)
    public ResponseEntity<Object> handleCustomParameterizedException(
        CustomParameterizedException ex,
        WebRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );

        problemDetail.setProperty(MESSAGE_FIELD, ex.getMessage());

        if (ex.getParamMap() != null) {
            problemDetail.setProperty("params", ex.getParamMap());
        }

        return new ResponseEntity<>(problemDetail, new HttpHeaders(), problemDetail.getStatus());
    }

    public ResponseEntity<ProblemDetail> process(ResponseEntity<ProblemDetail> entity, NativeWebRequest request) {
        if (entity == null) {
            return entity;
        }

        ProblemDetail problemDetail = entity.getBody();
        if (problemDetail == null) {
            return entity;
        }

        Map<String, Object> properties = problemDetail.getProperties();
        if (properties == null || !properties.containsKey(MESSAGE_FIELD)) {
            if (entity.getStatusCode().value() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                problemDetail.setProperty(MESSAGE_FIELD, "error.http.500");
            } else {
                problemDetail.setProperty(MESSAGE_FIELD, "error.http." + entity.getStatusCode().value());
            }
        }

        HttpServletRequest nativeRequest = request.getNativeRequest(HttpServletRequest.class);
        if (nativeRequest != null) {
            problemDetail.setProperty("path", nativeRequest.getRequestURI());
        }

        return new ResponseEntity<>(problemDetail, entity.getHeaders(), entity.getStatusCode());
    }

    private void addCommonProperties(ProblemDetail detail, WebRequest request) {
        detail.setProperty(MESSAGE_FIELD, "error.http." + detail.getStatus());
        addPathProperty(detail, request);
    }

    private void addPathProperty(ProblemDetail detail, WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            detail.setProperty("path", servletRequest.getRequest().getRequestURI());
        }
    }
}