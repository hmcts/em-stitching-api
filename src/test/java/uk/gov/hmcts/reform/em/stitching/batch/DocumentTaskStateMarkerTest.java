package uk.gov.hmcts.reform.em.stitching.batch;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DocumentTaskStateMarkerTest {

    private EntityManager entityManager;
    private DocumentTaskStateMarker documentTaskStateMarker;

    @BeforeEach
    void setUp() {
        entityManager = Mockito.mock(EntityManager.class);
        documentTaskStateMarker = new DocumentTaskStateMarker(entityManager);
    }

    @Test
    void markTaskAsInProgress_shouldSetTaskStateToInProgress() {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setTaskState(TaskState.NEW);

        documentTaskStateMarker.markTaskAsInProgress(documentTask);

        assertEquals(TaskState.IN_PROGRESS, documentTask.getTaskState());
        verify(entityManager, times(1)).merge(documentTask);
        verify(entityManager, times(1)).flush();
    }

    @Test
    void markTaskAsInProgress_shouldNotChangeStateIfAlreadyInProgress() {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setTaskState(TaskState.IN_PROGRESS);

        documentTaskStateMarker.markTaskAsInProgress(documentTask);

        assertEquals(TaskState.IN_PROGRESS, documentTask.getTaskState());
        verify(entityManager, times(1)).merge(documentTask);
        verify(entityManager, times(1)).flush();
    }

    @Test
    void markTaskAsInProgress_shouldHandleNullTaskState() {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setTaskState(null);

        documentTaskStateMarker.markTaskAsInProgress(documentTask);

        assertEquals(TaskState.IN_PROGRESS, documentTask.getTaskState());
        verify(entityManager, times(1)).merge(documentTask);
        verify(entityManager, times(1)).flush();
    }
}
