package uk.gov.hmcts.reform.em.stitching.rest.errors;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.LocaleResolver;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExceptionTranslatorTest {

    @Mock
    private MessageSource mockMessageSource;

    @Mock
    private LocaleResolver mockLocaleResolver;

    @Mock
    private NativeWebRequest mockNativeWebRequest;

    @Mock
    private HttpServletRequest mockHttpServletRequest;

    private ExceptionTranslator exceptionTranslator;

    private static final String DEFAULT_PATH = "/api/test";
    private static final String DEFAULT_ERROR_MESSAGE = "Default error message";

    @BeforeEach
    void setUp() {
        exceptionTranslator = new ExceptionTranslator(mockMessageSource, mockLocaleResolver);

        lenient().when(mockNativeWebRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(mockHttpServletRequest);
        lenient().when(mockNativeWebRequest.getNativeRequest()).thenReturn(mockHttpServletRequest);
        lenient().when(mockHttpServletRequest.getRequestURI()).thenReturn(DEFAULT_PATH);
        lenient().when(mockLocaleResolver.resolveLocale(mockHttpServletRequest)).thenReturn(Locale.ENGLISH);
        lenient().when(mockMessageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
            .thenReturn(DEFAULT_ERROR_MESSAGE);
    }

    @Test
    void processWithNullEntity() {
        ResponseEntity<Problem> result = exceptionTranslator.process(null, mockNativeWebRequest);
        assertNull(result);
    }

    @Test
    void processWithNonProblemSubclass() {
        Problem mockProblem = mock(Problem.class);
        ResponseEntity<Problem> originalEntity = ResponseEntity.ok(mockProblem);
        ResponseEntity<Problem> result = exceptionTranslator.process(originalEntity, mockNativeWebRequest);
        assertEquals(originalEntity, result);
    }

    @Test
    void processWithConstraintViolationProblem() {
        Violation mockViolation = new Violation("field", "message");
        URI zalandoConstraintViolationType = URI.create("https://zalando.github.io/problem/constraint-violation");
        ConstraintViolationProblem cvp =
            new ConstraintViolationProblem(zalandoConstraintViolationType, Status.BAD_REQUEST, List.of(mockViolation));
        ResponseEntity<Problem> originalEntity = new ResponseEntity<>(cvp, HttpStatus.BAD_REQUEST);

        ResponseEntity<Problem> result = exceptionTranslator.process(originalEntity, mockNativeWebRequest);

        assertNotNull(result);
        assertNotNull(result.getBody());
        Problem processedProblem = result.getBody();
        assertEquals(Status.BAD_REQUEST, processedProblem.getStatus());
        assertEquals(zalandoConstraintViolationType, processedProblem.getType());
        assertEquals("Constraint Violation", processedProblem.getTitle());
        assertEquals(DEFAULT_PATH, processedProblem.getParameters().get("path"));
        assertEquals(ErrorConstants.ERR_VALIDATION, processedProblem.getParameters().get("message"));
        assertInstanceOf(List.class, processedProblem.getParameters().get("violations"));
        assertEquals(1, ((List<?>) processedProblem.getParameters().get("violations")).size());
    }

    @Test
    void processWithDefaultProblemAndNoExistingMessage() {
        ThrowableProblem cause = Problem.builder().withStatus(Status.INTERNAL_SERVER_ERROR).build();
        Problem problem = Problem.builder()
            .withType(ErrorConstants.DEFAULT_TYPE)
            .withTitle("Internal Server Error")
            .withStatus(Status.INTERNAL_SERVER_ERROR)
            .withDetail("Detail message")
            .withCause(cause)
            .build();
        ResponseEntity<Problem> originalEntity = new ResponseEntity<>(problem, HttpStatus.INTERNAL_SERVER_ERROR);

        ResponseEntity<Problem> result = exceptionTranslator.process(originalEntity, mockNativeWebRequest);

        assertNotNull(result);
        Problem processedProblem = result.getBody();
        assertNotNull(processedProblem);
        assertEquals(Status.INTERNAL_SERVER_ERROR, processedProblem.getStatus());
        assertEquals(ErrorConstants.DEFAULT_TYPE, processedProblem.getType());
        assertEquals("Internal Server Error", processedProblem.getTitle());
        assertEquals("Detail message", processedProblem.getDetail());
        assertEquals(DEFAULT_PATH, processedProblem.getParameters().get("path"));
        assertInstanceOf(ThrowableProblem.class, processedProblem);
        assertEquals(cause, ((ThrowableProblem) processedProblem).getCause());
        assertEquals("error.http.500", processedProblem.getParameters().get("message"));
    }

    @Test
    void processWithDefaultProblemAndExistingMessage() {
        Problem problem = Problem.builder()
            .withStatus(Status.BAD_REQUEST)
            .withTitle("Bad Request")
            .with("message", "custom.error.key")
            .build();
        ResponseEntity<Problem> originalEntity = new ResponseEntity<>(problem, HttpStatus.BAD_REQUEST);

        ResponseEntity<Problem> result = exceptionTranslator.process(originalEntity, mockNativeWebRequest);

        assertNotNull(result);
        Problem processedProblem = result.getBody();
        assertNotNull(processedProblem);
        assertEquals("custom.error.key", processedProblem.getParameters().get("message"));
    }

    @Test
    void processWithoutHttpServletRequest() {
        when(mockNativeWebRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(null);
        Problem problem = Problem.builder().withStatus(Status.BAD_REQUEST).build();
        ResponseEntity<Problem> originalEntity = new ResponseEntity<>(problem, HttpStatus.BAD_REQUEST);

        ResponseEntity<Problem> result = exceptionTranslator.process(originalEntity, mockNativeWebRequest);
        assertNotNull(result);
        Problem processedProblem = result.getBody();
        assertNotNull(processedProblem);
        assertNull(processedProblem.getParameters().get("path"));
    }

    @Test
    void handleMethodArgumentNotValid() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult mockBindingResult = mock(BindingResult.class);
        FieldError mockFieldError = new FieldError("objectName", "fieldName", "rejectedValue", false,
            new String[]{"errorCode1"}, new Object[]{}, "default message from field error");

        when(ex.getBindingResult()).thenReturn(mockBindingResult);
        when(mockBindingResult.getFieldErrors()).thenReturn(List.of(mockFieldError));
        when(mockMessageSource.getMessage("errorCode1", null, "default message from field error", Locale.ENGLISH))
            .thenReturn("Translated field error message");

        ResponseEntity<Problem> responseEntity = exceptionTranslator.handleMethodArgumentNotValid(ex, mockNativeWebRequest);

        assertNotNull(responseEntity);
        Problem problem = responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(ErrorConstants.CONSTRAINT_VIOLATION_TYPE, problem.getType());
        assertEquals("Method argument not valid", problem.getTitle());
        assertEquals(Status.BAD_REQUEST, problem.getStatus());
        assertEquals(ErrorConstants.ERR_VALIDATION, problem.getParameters().get("message"));
        assertInstanceOf(List.class, problem.getParameters().get("fieldErrors"));
        @SuppressWarnings("unchecked")
        List<FieldErrorVM> fieldErrors = (List<FieldErrorVM>) problem.getParameters().get("fieldErrors");
        assertEquals(1, fieldErrors.size());
        assertEquals("objectName", fieldErrors.getFirst().getObjectName());
        assertEquals("fieldName", fieldErrors.getFirst().getField());
        assertEquals("Translated field error message", fieldErrors.getFirst().getMessage());
    }

    @Test
    void handleNoSuchElementException() {
        NoSuchElementException ex = new NoSuchElementException("Test not found");
        ResponseEntity<Problem> responseEntity = exceptionTranslator.handleNoSuchElementException(ex, mockNativeWebRequest);

        assertNotNull(responseEntity);
        Problem problem = responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(Status.NOT_FOUND, problem.getStatus());
        assertEquals(ErrorConstants.ENTITY_NOT_FOUND_TYPE, problem.getParameters().get("message"));
        assertEquals(DEFAULT_PATH, problem.getParameters().get("path"));
    }

    @Test
    void handleBadRequestAlertException() {
        BadRequestAlertException ex = new BadRequestAlertException("Error message", "entityName", "errorKey");
        ResponseEntity<Problem> responseEntity = exceptionTranslator.handleBadRequestAlertException(ex, mockNativeWebRequest);

        assertNotNull(responseEntity);
        Problem problem = responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(Status.BAD_REQUEST, problem.getStatus());
    }

    @Test
    void handleConcurrencyFailure() {
        ConcurrencyFailureException ex = new ConcurrencyFailureException("Concurrency error");
        ResponseEntity<Problem> responseEntity = exceptionTranslator.handleConcurrencyFailure(ex, mockNativeWebRequest);

        assertNotNull(responseEntity);
        Problem problem = responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(Status.CONFLICT, problem.getStatus());
        assertEquals(ErrorConstants.ERR_CONCURRENCY_FAILURE, problem.getParameters().get("message"));
        assertEquals(DEFAULT_PATH, problem.getParameters().get("path"));
    }

    @Test
    void handleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        ResponseEntity<Problem> responseEntity = exceptionTranslator.handleAccessDenied(ex, mockNativeWebRequest);

        assertNotNull(responseEntity);
        Problem problem = responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(Status.FORBIDDEN, problem.getStatus());
        assertEquals(ErrorConstants.ERR_FORBIDDEN, problem.getParameters().get("message"));
        assertEquals(DEFAULT_PATH, problem.getParameters().get("path"));
    }

    @Test
    void handleUnauthorised() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<Problem> responseEntity = exceptionTranslator.handleUnAuthorised(ex, mockNativeWebRequest);

        assertNotNull(responseEntity);
        Problem problem = responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(Status.UNAUTHORIZED, problem.getStatus());
        assertEquals(ErrorConstants.ERR_UNAUTHORISED, problem.getParameters().get("message"));
        assertEquals(DEFAULT_PATH, problem.getParameters().get("path"));
    }
}