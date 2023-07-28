package uk.gov.hmcts.reform.em.stitching.rest.errors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.LocaleResolver;
import org.zalando.problem.DefaultProblem;
import org.zalando.problem.Problem;
import org.zalando.problem.ProblemBuilder;
import org.zalando.problem.Status;
import org.zalando.problem.spring.web.advice.ProblemHandling;
import org.zalando.problem.violations.ConstraintViolationProblem;
import uk.gov.hmcts.reform.em.stitching.rest.util.HeaderUtil;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 * The error response follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807)
 */
@ControllerAdvice
public class ExceptionTranslator implements ProblemHandling {

    private static final String MSG = "message";
    @Autowired
    private MessageSource messageSource;

    @Autowired
    private LocaleResolver localeResolver;

    /**
     * Post-process the Problem payload to add the message key for the front-end if needed.
     */
    @Override
    public ResponseEntity<Problem> process(@Nullable ResponseEntity<Problem> entity, NativeWebRequest request) {
        if (entity == null) {
            return entity;
        }
        Problem problem = entity.getBody();
        if (!(problem instanceof ConstraintViolationProblem || problem instanceof DefaultProblem)) {
            return entity;
        }
        ProblemBuilder builder = Problem.builder()
            .withType(Problem.DEFAULT_TYPE.equals(problem.getType()) ? ErrorConstants.DEFAULT_TYPE : problem.getType())
            .withStatus(problem.getStatus())
            .withTitle(problem.getTitle());

        if (Objects.nonNull(request)) {
            HttpServletRequest httpServletRequest = request.getNativeRequest(HttpServletRequest.class);
            if (Objects.nonNull(httpServletRequest)) {
                builder.with("path", httpServletRequest.getRequestURI());
            }
        }

        if (problem instanceof ConstraintViolationProblem) {
            builder
                .with("violations", ((ConstraintViolationProblem) problem).getViolations())
                .with(MSG, ErrorConstants.ERR_VALIDATION);
        } else {
            builder
                .withCause(((DefaultProblem) problem).getCause())
                .withDetail(problem.getDetail())
                .withInstance(problem.getInstance());
            problem.getParameters().forEach(builder::with);
            if (!problem.getParameters().containsKey(MSG) && problem.getStatus() != null) {
                builder.with(MSG, "error.http." + problem.getStatus().getStatusCode());
            }
        }
        return new ResponseEntity<>(builder.build(), entity.getHeaders(), entity.getStatusCode());
    }

    @Override
    public ResponseEntity<Problem> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @Nonnull NativeWebRequest request) {
        BindingResult result = ex.getBindingResult();
        List<FieldErrorVM> fieldErrors = result.getFieldErrors().stream()
            .map(f -> new FieldErrorVM(f.getObjectName(), f.getField(),
                    messageSource.getMessage(f.getCode(), null, f.getDefaultMessage(),
                        localeResolver.resolveLocale((HttpServletRequest)request.getNativeRequest()))))
            .collect(Collectors.toList());

        Problem problem = Problem.builder()
            .withType(ErrorConstants.CONSTRAINT_VIOLATION_TYPE)
            .withTitle("Method argument not valid")
            .withStatus(defaultConstraintViolationStatus())
            .with(MSG, ErrorConstants.ERR_VALIDATION)
            .with("fieldErrors", fieldErrors)
            .build();
        return create(ex, problem, request);
    }

    @ExceptionHandler
    public ResponseEntity<Problem> handleNoSuchElementException(NoSuchElementException ex, NativeWebRequest request) {
        Problem problem = Problem.builder()
            .withStatus(Status.NOT_FOUND)
            .with(MSG, ErrorConstants.ENTITY_NOT_FOUND_TYPE)
            .build();
        return create(ex, problem, request);
    }

    @ExceptionHandler
    public ResponseEntity<Problem> handleBadRequestAlertException(
            BadRequestAlertException ex,
            NativeWebRequest request) {
        return create(ex, request, HeaderUtil.createFailureAlert(
                ex.getEntityName(),
                ex.getErrorKey(),
                ex.getMessage())
        );
    }

    @ExceptionHandler
    public ResponseEntity<Problem> handleConcurrencyFailure(ConcurrencyFailureException ex, NativeWebRequest request) {
        Problem problem = Problem.builder()
            .withStatus(Status.CONFLICT)
            .with(MSG, ErrorConstants.ERR_CONCURRENCY_FAILURE)
            .build();
        return create(ex, problem, request);
    }

    @ExceptionHandler
    public ResponseEntity<Problem> handleAccessDenied(AccessDeniedException ex, NativeWebRequest request) {
        Problem problem = Problem.builder()
            .withStatus(Status.FORBIDDEN)
            .with(MSG, ErrorConstants.ERR_FORBIDDEN)
            .build();
        return create(ex, problem, request);
    }

    @ExceptionHandler
    public ResponseEntity<Problem> handleUnAuthorised(BadCredentialsException ex, NativeWebRequest request) {
        Problem problem = Problem.builder()
            .withStatus(Status.UNAUTHORIZED)
            .with(MSG, ErrorConstants.ERR_UNAUTHORISED)
            .build();
        return create(ex, problem, request);
    }
}
