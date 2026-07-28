package uk.gov.hmcts.reform.em.stitching.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.em.stitching.repository.EntityAuditEventRepository;

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
class RemoveOldAuditTaskTest {

    @Mock
    private EntityAuditEventRepository entityAuditEventRepository;

    @InjectMocks
    private RemoveOldAuditTask removeOldAuditTask;

    private final int numberOfDays = 30;
    private final int numberOfRecords = 100;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(removeOldAuditTask, "numberOfDays", numberOfDays);
        ReflectionTestUtils.setField(removeOldAuditTask, "numberOfRecords", numberOfRecords);
    }

    @Test
    void shouldDeleteRecordsWhenFound() {
        List<Long> mockAuditIds = List.of(1L, 2L, 3L);
        when(entityAuditEventRepository.findAllByModifiedDate(any(Instant.class), eq(numberOfRecords)))
            .thenReturn(mockAuditIds);

        removeOldAuditTask.run();

        verify(entityAuditEventRepository).findAllByModifiedDate(any(Instant.class), eq(numberOfRecords));
        verify(entityAuditEventRepository).deleteAllById(mockAuditIds);
    }

    @Test
    void shouldNotAttemptDeletionWhenNoRecordsFound() {
        when(entityAuditEventRepository.findAllByModifiedDate(any(Instant.class), eq(numberOfRecords)))
            .thenReturn(Collections.emptyList());

        removeOldAuditTask.run();

        verify(entityAuditEventRepository).findAllByModifiedDate(any(Instant.class), eq(numberOfRecords));
        verify(entityAuditEventRepository, never()).deleteAllById(any());
    }

    @Test
    void shouldNotAttemptDeletionWhenRecordsListIsNull() {
        when(entityAuditEventRepository.findAllByModifiedDate(any(Instant.class), eq(numberOfRecords)))
            .thenReturn(null);

        removeOldAuditTask.run();

        verify(entityAuditEventRepository).findAllByModifiedDate(any(Instant.class), eq(numberOfRecords));
        verify(entityAuditEventRepository, never()).deleteAllById(any());
    }

    @Test
    void shouldHandleExceptionGracefully() {
        when(entityAuditEventRepository.findAllByModifiedDate(any(Instant.class), eq(numberOfRecords)))
            .thenThrow(new RuntimeException("Database connection error"));

        assertDoesNotThrow(() -> removeOldAuditTask.run());

        verify(entityAuditEventRepository).findAllByModifiedDate(any(Instant.class), eq(numberOfRecords));
        verify(entityAuditEventRepository, never()).deleteAllById(any());
    }
}