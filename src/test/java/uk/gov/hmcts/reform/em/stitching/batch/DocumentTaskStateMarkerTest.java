package uk.gov.hmcts.reform.em.stitching.batch;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentTaskStateMarkerTest {

    @Mock
    private DocumentTaskRepository documentTaskRepository;

    private DocumentTaskStateMarker documentTaskStateMarker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        documentTaskStateMarker = new DocumentTaskStateMarker(documentTaskRepository);
    }

    @Test
    void testCommitTaskFromNew() {
        final Long taskId = 1L;
        DocumentTask documentTask = new DocumentTask();
        documentTask.setId(taskId);
        documentTask.setTaskState(TaskState.NEW);

        when(documentTaskRepository.findById(taskId)).thenReturn(Optional.of(documentTask));

        documentTaskStateMarker.commitTaskAsInProgress(taskId);

        assertEquals(TaskState.IN_PROGRESS, documentTask.getTaskState());
        verify(documentTaskRepository).findById(taskId);
    }

    @Test
    void testCommitTaskAlreadyInProgress() {
        final Long taskId = 2L;
        DocumentTask documentTask = new DocumentTask();
        documentTask.setId(taskId);
        documentTask.setTaskState(TaskState.IN_PROGRESS);

        when(documentTaskRepository.findById(taskId)).thenReturn(Optional.of(documentTask));

        documentTaskStateMarker.commitTaskAsInProgress(taskId);

        assertEquals(TaskState.IN_PROGRESS, documentTask.getTaskState());
        verify(documentTaskRepository).findById(taskId);
    }

    @Test
    void testCommitTaskFromNullState() {
        final Long taskId = 3L;
        DocumentTask documentTask = new DocumentTask();
        documentTask.setId(taskId);
        documentTask.setTaskState(null);

        when(documentTaskRepository.findById(taskId)).thenReturn(Optional.of(documentTask));

        documentTaskStateMarker.commitTaskAsInProgress(taskId);

        assertEquals(TaskState.IN_PROGRESS, documentTask.getTaskState());
        verify(documentTaskRepository).findById(taskId);
    }

    @Test
    void testCommitTaskThrowsEntityNotFound() {
        final Long nonExistentTaskId = 404L;
        when(documentTaskRepository.findById(nonExistentTaskId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            documentTaskStateMarker.commitTaskAsInProgress(nonExistentTaskId);
        });

        verify(documentTaskRepository).findById(nonExistentTaskId);
    }
}