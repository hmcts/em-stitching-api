package uk.gov.hmcts.reform.em.stitching.rest.errors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.LocaleResolver;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        lenient().when(mockNativeWebRequest.getNativeRequest(HttpServletRequest.class))
            .thenReturn(mockHttpServletRequest);
        lenient().when(mockHttpServletRequest.getRequestURI()).thenReturn(DEFAULT_PATH);
        lenient().when(mockLocaleResolver.resolveLocale(mockHttpServletRequest)).thenReturn(Locale.ENGLISH);
        lenient().when(mockMessageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
            .thenReturn(DEFAULT_ERROR_MESSAGE);
    }

    @Test
    void processWithNullEntity() {
        ResponseEntity<ProblemDetail> result = exceptionTranslator.process(null, mockNativeWebRequest);
        assertNull(result);
    }

    @Test
    void processWithDefaultProblemAndNoExistingMessage() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Detail message");
        problem.setTitle("Internal Server Error");
        ResponseEntity<ProblemDetail> originalEntity = new ResponseEntity<>(problem, HttpStatus.INTERNAL_SERVER_ERROR);

        ResponseEntity<ProblemDetail> result = exceptionTranslator.process(originalEntity, mockNativeWebRequest);

        assertNotNull(result);
        ProblemDetail processedProblem = result.getBody();
        assertNotNull(processedProblem);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), processedProblem.getStatus());
        assertEquals("Internal Server Error", processedProblem.getTitle());
        assertEquals("Detail message", processedProblem.getDetail());
        assertEquals(DEFAULT_PATH, processedProblem.getProperties().get("path"));
        assertEquals("error.http.500", processedProblem.getProperties().get("message"));
    }

    @Test
    void processWithDefaultProblemAndExistingMessage() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "");
        problem.setProperty("message", "custom.error.key");
        ResponseEntity<ProblemDetail> originalEntity = new ResponseEntity<>(problem, HttpStatus.BAD_REQUEST);

        ResponseEntity<ProblemDetail> result = exceptionTranslator.process(originalEntity, mockNativeWebRequest);

        assertNotNull(result);
        ProblemDetail processedProblem = result.getBody();
        assertNotNull(processedProblem);
        assertEquals("custom.error.key", processedProblem.getProperties().get("message"));
    }

    @Test
    void processWithoutHttpServletRequest() {
        when(mockNativeWebRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(null);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "");
        ResponseEntity<ProblemDetail> originalEntity = new ResponseEntity<>(problem, HttpStatus.BAD_REQUEST);

        ResponseEntity<ProblemDetail> result = exceptionTranslator.process(originalEntity, mockNativeWebRequest);
        assertNotNull(result);
        ProblemDetail processedProblem = result.getBody();
        assertNotNull(processedProblem);
        // Using assertNull directly fails if getProperties() returns null initially, so we check safely:
        if (processedProblem.getProperties() != null) {
            assertNull(processedProblem.getProperties().get("path"));
        }
    }

    @Test
    void handleConstraintViolationException() {
        ConstraintViolation<?> mockViolation = mock(ConstraintViolation.class);
        Path mockPath = mock(Path.class);
        when(mockViolation.getPropertyPath()).thenReturn(mockPath);
        when(mockPath.toString()).thenReturn("some.field");
        lenient().doReturn(String.class).when(mockViolation).getRootBeanClass();
        when(mockViolation.getMessage()).thenReturn("must not be null");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(mockViolation));

        ResponseEntity<Object> responseEntity =
            exceptionTranslator.handleConstraintViolationException(ex, mockNativeWebRequest);

        assertNotNull(responseEntity);
        ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("Constraint violation", problem.getTitle());
        assertEquals(ErrorConstants.CONSTRAINT_VIOLATION_TYPE, problem.getType());
        assertEquals(ErrorConstants.ERR_VALIDATION, problem.getProperties().get("message"));

        @SuppressWarnings("unchecked")
        List<FieldErrorVM> fieldErrors = (List<FieldErrorVM>) problem.getProperties().get("fieldErrors");
        assertEquals(1, fieldErrors.size());
        assertEquals("String", fieldErrors.get(0).getObjectName());
        assertEquals("field", fieldErrors.get(0).getField());
        assertEquals("must not be null", fieldErrors.get(0).getMessage());
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

        ResponseEntity<Object> responseEntity = exceptionTranslator.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, mockNativeWebRequest);

        assertNotNull(responseEntity);
        ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(ErrorConstants.CONSTRAINT_VIOLATION_TYPE, problem.getType());
        assertEquals("Method argument not valid", problem.getTitle());
        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals(ErrorConstants.ERR_VALIDATION, problem.getProperties().get("message"));

        @SuppressWarnings("unchecked")
        List<FieldErrorVM> fieldErrors = (List<FieldErrorVM>) problem.getProperties().get("fieldErrors");
        assertEquals(1, fieldErrors.size());
        assertEquals("objectName", fieldErrors.get(0).getObjectName());
        assertEquals("fieldName", fieldErrors.get(0).getField());
        assertEquals("Translated field error message", fieldErrors.get(0).getMessage());
    }

    @Test
    void handleHttpRequestMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        ResponseEntity<Object> responseEntity = exceptionTranslator.handleHttpRequestMethodNotSupported(
            ex, new HttpHeaders(), HttpStatus.METHOD_NOT_ALLOWED, mockNativeWebRequest);

        assertNotNull(responseEntity);
        ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), problem.getStatus());
        assertEquals("error.http.405", problem.getProperties().get("message"));
        assertEquals("Request method 'POST' is not supported", problem.getProperties().get("detail"));
    }

    @Test
    void handleMissingServletRequestParameter() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("param", "String");
        ResponseEntity<Object> responseEntity = exceptionTranslator.handleMissingServletRequestParameter(
            ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, mockNativeWebRequest);

        assertNotNull(responseEntity);
        ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("error.http.400", problem.getProperties().get("message"));
    }

    @Test
    void handleMissingServletRequestPart() {
        MissingServletRequestPartException ex = new MissingServletRequestPartException("partName");
        ResponseEntity<Object> responseEntity = exceptionTranslator.handleMissingServletRequestPart(
            ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, mockNativeWebRequest);

        assertNotNull(responseEntity);
        ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("error.http.400", problem.getProperties().get("message"));
    }

    @Test
    void handleNoSuchElementException() {
        NoSuchElementException ex = new NoSuchElementException("Test not found");
        ResponseEntity<Object> responseEntity =
            exceptionTranslator.handleNoSuchElementException(ex, mockNativeWebRequest);

        assertNotNull(responseEntity);
        ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
        assertEquals(ErrorConstants.ENTITY_NOT_FOUND_TYPE, problem.getProperties().get("message"));
    }

    @Test
    void handleBadRequestAlertException() {
        BadRequestAlertException ex = new BadRequestAlertException("Error message", "entityName", "errorKey");
        ResponseEntity<Object> responseEntity =
            exceptionTranslator.handleBadRequestAlertException(ex, mockNativeWebRequest);

        assertNotNull(responseEntity);
        ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("error.errorKey", problem.getProperties().get("message"));
        assertFalse(responseEntity.getHeaders().isEmpty()); // Validates HeaderUtil populated response headers
    }

    @Test
    void handleBindException() {
        BindException ex = new BindException("message", "objectName");
        ResponseEntity<Object> responseEntity =
            exceptionTranslator.handleBindException(ex, mockNativeWebRequest);
        assertNotNull(responseEntity);
        ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals(ErrorConstants.BAD_REQUEST, problem.getProperties().get("message"));
    }

    @Test
    void handleConcurrencyFailure() {
        ConcurrencyFailureException ex = new ConcurrencyFailureException("Concurrency error");
        ResponseEntity<Object> responseEntity = exceptionTranslator.handleConcurrencyFailure(ex, mockNativeWebRequest);

        assertNotNull(responseEntity);
        ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
        assertEquals(ErrorConstants.ERR_CONCURRENCY_FAILURE, problem.getProperties().get("message"));
    }

    @Test
    void handleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        ResponseEntity<Object> responseEntity = exceptionTranslator.handleAccessDenied(ex, mockNativeWebRequest);

        assertNotNull(responseEntity);
        ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(HttpStatus.FORBIDDEN.value(), problem.getStatus());
        assertEquals(ErrorConstants.ERR_FORBIDDEN, problem.getProperties().get("message"));
    }

    @Test
    void handleUnauthorised() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<Object> responseEntity = exceptionTranslator.handleUnAuthorised(ex, mockNativeWebRequest);

        assertNotNull(responseEntity);
        ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), problem.getStatus());
        assertEquals(ErrorConstants.ERR_UNAUTHORISED, problem.getProperties().get("message"));
        assertEquals(DEFAULT_PATH, problem.getProperties().get("path"));
    }

    @Test
    void handleUnexpectedRuntimeWithoutResponseStatus() {
        RuntimeException ex = new RuntimeException("Unexpected error");
        ServletWebRequest request = new ServletWebRequest(mockHttpServletRequest);

        ResponseEntity<Object> responseEntity = exceptionTranslator.handleUnexpectedRuntime(ex, request);

        assertNotNull(responseEntity);
        ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals("An unexpected internal server error occurred.", problem.getDetail());
        assertEquals("error.http.500", problem.getProperties().get("message"));
        assertEquals(DEFAULT_PATH, problem.getProperties().get("path"));
    }

    @ResponseStatus(value = HttpStatus.BAD_GATEWAY, reason = "Bad Gateway Reason")
    static class TestResponseStatusException extends RuntimeException {
        TestResponseStatusException(String message) {
            super(message);
        }
    }

    @Test
    void handleUnexpectedRuntimeWithResponseStatus() {
        TestResponseStatusException ex = new TestResponseStatusException("Custom exception message");
        ServletWebRequest request = new ServletWebRequest(mockHttpServletRequest);

        ResponseEntity<Object> responseEntity = exceptionTranslator.handleUnexpectedRuntime(ex, request);

        assertNotNull(responseEntity);
        ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(HttpStatus.BAD_GATEWAY.value(), problem.getStatus());
        assertEquals("Bad Gateway Reason", problem.getTitle());
        assertEquals("Custom exception message", problem.getDetail());
        assertEquals("error.http.502", problem.getProperties().get("message"));
        assertEquals(DEFAULT_PATH, problem.getProperties().get("path"));
    }

    @Test
    void handleCustomParameterizedException() {
        CustomParameterizedException ex = new CustomParameterizedException("my error message", "param1", "param2");
        ResponseEntity<Object> responseEntity =
            exceptionTranslator.handleCustomParameterizedException(ex, mockNativeWebRequest);

        assertNotNull(responseEntity);
        ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
        assertNotNull(problem);
        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("my error message", problem.getProperties().get("message"));
        assertNotNull(problem.getProperties().get("params"));
    }
}