package uk.gov.hmcts.reform.em.stitching.rest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.rest.errors.BadRequestAlertException;
import uk.gov.hmcts.reform.em.stitching.service.DocumentTaskService;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentTaskResourceTest {

    @Mock
    private DocumentTaskService documentTaskService;

    @Mock
    private HttpServletRequest mockHttpServletRequest;

    @InjectMocks
    private DocumentTaskResource documentTaskResource;

    private DocumentTaskDTO documentTaskDTO;
    private static final String AUTH_HEADER_VALUE = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciJ9.testtoken";
    private static final Long DEFAULT_ID = 1L;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(DocumentTaskResource.class);
        logger.setLevel(Level.DEBUG);

        documentTaskDTO = new DocumentTaskDTO();
        documentTaskDTO.setBundle(new BundleDTO());
        documentTaskDTO.getBundle().setBundleTitle("Test Bundle");
        lenient().when(mockHttpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    }

    @Test
    void createDocumentTaskSuccess() throws URISyntaxException, DocumentTaskProcessingException {
        documentTaskDTO.setId(null);

        when(documentTaskService.save(any(DocumentTaskDTO.class))).thenAnswer(invocation -> {
            DocumentTaskDTO argument = invocation.getArgument(0);
            argument.setId(DEFAULT_ID);
            argument.setTaskState(TaskState.NEW);
            argument.setJwt(AUTH_HEADER_VALUE);
            return argument;
        });

        ResponseEntity<DocumentTaskDTO> response = documentTaskResource.createDocumentTask(
            documentTaskDTO, AUTH_HEADER_VALUE, mockHttpServletRequest
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(DEFAULT_ID, response.getBody().getId());
        assertEquals(TaskState.NEW, response.getBody().getTaskState());
        assertEquals(AUTH_HEADER_VALUE, response.getBody().getJwt());
        assertEquals("/api/document-tasks/" + DEFAULT_ID, response.getHeaders().getLocation().getPath());

        verify(documentTaskService).save(any(DocumentTaskDTO.class));
    }

    @Test
    void createDocumentTaskIdAlreadyExists() {
        documentTaskDTO.setId(DEFAULT_ID);

        BadRequestAlertException exception = assertThrows(BadRequestAlertException.class, () ->
            documentTaskResource.createDocumentTask(documentTaskDTO, AUTH_HEADER_VALUE, mockHttpServletRequest)
        );

        assertEquals("A new documentTask cannot already have an ID", exception.getMessage());
        assertEquals("documentTask", exception.getEntityName());
        assertEquals("id exists", exception.getErrorKey());
        verify(documentTaskService, never()).save(any());
    }

    static Stream<Arguments> exceptionScenarios() {
        RuntimeException runtimeEx = new RuntimeException("Database unavailable");
        String expectedMsg1 = "Error saving Document Task : " + runtimeEx + " Caused by " + runtimeEx;

        Throwable actualRootCause = new IllegalArgumentException("Actual underlying problem");
        RuntimeException wrapperEx = new RuntimeException("Service layer error", actualRootCause);
        String expectedMsg2 = "Error saving Document Task : " + wrapperEx + " Caused by " + actualRootCause;

        ConstraintViolation<?> mockViolation = mock(ConstraintViolation.class);
        when(mockViolation.getMessage()).thenReturn("Bundle title cannot be empty");
        Set<ConstraintViolation<?>> violations = new HashSet<>(Set.of(mockViolation));
        ConstraintViolationException cve = new ConstraintViolationException("Validation failed", violations);
        String expectedMsg3 = "Error saving Document Task : " + cve
            + " Caused by ConstraintViolationException :  Bundle title cannot be empty";

        return Stream.of(
            Arguments.of("RuntimeException is root", runtimeEx, expectedMsg1, runtimeEx),
            Arguments.of("Nested RuntimeException", wrapperEx, expectedMsg2, wrapperEx),
            Arguments.of("ConstraintViolationException", cve, expectedMsg3, null)
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("exceptionScenarios")
    void serviceErrorThrowsDocumentTaskProcessingException(
        String scenarioName,
        Throwable serviceExceptionToThrow,
        String expectedDtpMessage,
        Throwable expectedDtpCause) {

        documentTaskDTO.setId(null);
        when(documentTaskService.save(any(DocumentTaskDTO.class))).thenThrow(serviceExceptionToThrow);

        DocumentTaskProcessingException exception = assertThrows(DocumentTaskProcessingException.class, () ->
            documentTaskResource.createDocumentTask(documentTaskDTO, AUTH_HEADER_VALUE, mockHttpServletRequest)
        );

        assertEquals(expectedDtpMessage, exception.getMessage());
        if (Objects.isNull(expectedDtpCause)) {
            assertNull(exception.getCause());
        } else {
            assertEquals(expectedDtpCause, exception.getCause());
        }
    }

    @Test
    void getDocumentTaskFound() {
        documentTaskDTO.setId(DEFAULT_ID);
        documentTaskDTO.setTaskState(TaskState.DONE);
        when(documentTaskService.findOne(DEFAULT_ID)).thenReturn(Optional.of(documentTaskDTO));

        ResponseEntity<DocumentTaskDTO> response = documentTaskResource.getDocumentTask(DEFAULT_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(DEFAULT_ID, response.getBody().getId());
        assertEquals(TaskState.DONE, response.getBody().getTaskState());
        verify(documentTaskService).findOne(DEFAULT_ID);
    }

    @Test
    void getDocumentTaskNotFound() {
        when(documentTaskService.findOne(anyLong())).thenReturn(Optional.empty());

        ResponseEntity<DocumentTaskDTO> response = documentTaskResource.getDocumentTask(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(documentTaskService).findOne(99L);
    }
}