package uk.gov.hmcts.reform.em.stitching.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveOldDocumentTaskTest {

    @Mock
    private DocumentTaskRepository documentTaskRepository;

    @InjectMocks
    private RemoveOldDocumentTask removeOldDocumentTask;

    private final int numberOfDays = 30;
    private final int numberOfRecords = 100;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(removeOldDocumentTask, "numberOfDays", numberOfDays);
        ReflectionTestUtils.setField(removeOldDocumentTask, "numberOfRecords", numberOfRecords);
    }

    @Test
    void shouldDeleteRecordsWhenFound() {
        List<Long> mockTaskIds = List.of(1L, 2L, 3L);
        when(documentTaskRepository.findAllByCreatedDate(any(Instant.class), eq(numberOfRecords)))
            .thenReturn(mockTaskIds);

        removeOldDocumentTask.run();

        verify(documentTaskRepository).findAllByCreatedDate(any(Instant.class), eq(numberOfRecords));
        verify(documentTaskRepository).deleteAllById(mockTaskIds);
    }

    @Test
    void shouldNotAttemptDeletionWhenNoRecordsFound() {
        when(documentTaskRepository.findAllByCreatedDate(any(Instant.class), eq(numberOfRecords)))
            .thenReturn(Collections.emptyList());

        removeOldDocumentTask.run();

        verify(documentTaskRepository).findAllByCreatedDate(any(Instant.class), eq(numberOfRecords));
        verify(documentTaskRepository, never()).deleteAllById(any());
    }

    @Test
    void shouldNotAttemptDeletionWhenRecordsListIsNull() {
        when(documentTaskRepository.findAllByCreatedDate(any(Instant.class), eq(numberOfRecords)))
            .thenReturn(null);

        removeOldDocumentTask.run();

        verify(documentTaskRepository).findAllByCreatedDate(any(Instant.class), eq(numberOfRecords));
        verify(documentTaskRepository, never()).deleteAllById(any());
    }

    @Test
    void shouldHandleExceptionGracefully() {
        when(documentTaskRepository.findAllByCreatedDate(any(Instant.class), eq(numberOfRecords)))
            .thenThrow(new RuntimeException("Database connection error"));

        assertDoesNotThrow(() -> removeOldDocumentTask.run());

        verify(documentTaskRepository).findAllByCreatedDate(any(Instant.class), eq(numberOfRecords));
        verify(documentTaskRepository, never()).deleteAllById(any());
    }
}