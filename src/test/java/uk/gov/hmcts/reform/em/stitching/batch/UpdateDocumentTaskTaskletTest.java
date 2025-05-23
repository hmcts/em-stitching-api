package uk.gov.hmcts.reform.em.stitching.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateDocumentTaskTaskletTest {

    @Mock
    private DocumentTaskRepository documentTaskRepository;

    @Mock
    private StepContribution contribution;

    @Mock
    private ChunkContext chunkContext;

    private UpdateDocumentTaskTasklet updateDocumentTaskTasklet;

    @Test
    void executeZeroRecords() {
        updateDocumentTaskTasklet = new UpdateDocumentTaskTasklet(documentTaskRepository, 0);
        when(documentTaskRepository.findAllByTaskStatus(anyString(), anyInt())).thenReturn(Collections.emptyList());

        updateDocumentTaskTasklet.execute(contribution, chunkContext);

        verify(documentTaskRepository, times(1)).findAllByTaskStatus(anyString(), anyInt());
        verify(documentTaskRepository, never()).findAllById(any());
        verify(documentTaskRepository, never()).saveAll(any());
    }

    @Test
    void executeFiveRecords() {
        updateDocumentTaskTasklet = new UpdateDocumentTaskTasklet(documentTaskRepository, 5);
        when(documentTaskRepository.findAllByTaskStatus(anyString(), anyInt())).thenReturn(List.of(5L));
        when(documentTaskRepository.findAllById(any())).thenReturn(List.of(new DocumentTask()));

        updateDocumentTaskTasklet.execute(contribution, chunkContext);

        verify(documentTaskRepository, times(1)).findAllByTaskStatus(anyString(), anyInt());
        verify(documentTaskRepository, times(1)).findAllById(any());
        verify(documentTaskRepository, times(1)).saveAll(any());
    }

    @Test
    void executeDoesNotSaveWhenTasksFoundByStatusButNotByIds() {
        int batchSize = 5;
        List<Long> taskIds = Arrays.asList(1L, 2L);
        updateDocumentTaskTasklet = new UpdateDocumentTaskTasklet(documentTaskRepository, batchSize);

        when(documentTaskRepository.findAllByTaskStatus(TaskState.NEW.toString(), batchSize))
            .thenReturn(taskIds);
        when(documentTaskRepository.findAllById(taskIds))
            .thenReturn(Collections.emptyList());

        updateDocumentTaskTasklet.execute(contribution, chunkContext);

        verify(documentTaskRepository).findAllByTaskStatus(TaskState.NEW.toString(), batchSize);
        verify(documentTaskRepository).findAllById(taskIds);
        verify(documentTaskRepository, never()).saveAll(any());
    }

    @Test
    void executeMarksTaskAsFailedWhenJurisdictionIdIsNull() {
        int batchSize = 1;
        long taskId = 10L;
        DocumentTask taskWithoutJurisdiction = new DocumentTask();
        updateDocumentTaskTasklet = new UpdateDocumentTaskTasklet(documentTaskRepository, batchSize);

        when(documentTaskRepository.findAllByTaskStatus(TaskState.NEW.toString(), batchSize))
            .thenReturn(Collections.singletonList(taskId));
        when(documentTaskRepository.findAllById(Collections.singletonList(taskId)))
            .thenReturn(Collections.singletonList(taskWithoutJurisdiction));

        final ArgumentCaptor<Iterable<DocumentTask>> captor = ArgumentCaptor.captor();
        updateDocumentTaskTasklet.execute(contribution, chunkContext);

        verify(documentTaskRepository).findAllByTaskStatus(TaskState.NEW.toString(), batchSize);
        verify(documentTaskRepository).findAllById(Collections.singletonList(taskId));
        verify(documentTaskRepository).saveAll(captor.capture());

        List<DocumentTask> savedTasks = new ArrayList<>();
        captor.getValue().forEach(savedTasks::add);

        assertEquals(1, savedTasks.size());
        DocumentTask savedTask = savedTasks.getFirst();
        assertEquals(TaskState.FAILED, savedTask.getTaskState());
    }

    @Test
    void executePreservesTaskStateWhenJurisdictionIdIsPresent() {
        final int batchSize = 1;
        final long taskId = 20L;
        DocumentTask taskWithJurisdiction = new DocumentTask();
        taskWithJurisdiction.setJurisdictionId("someJurisdiction");
        taskWithJurisdiction.setTaskState(TaskState.NEW);
        updateDocumentTaskTasklet = new UpdateDocumentTaskTasklet(documentTaskRepository, batchSize);

        when(documentTaskRepository.findAllByTaskStatus(TaskState.NEW.toString(), batchSize))
            .thenReturn(Collections.singletonList(taskId));
        when(documentTaskRepository.findAllById(Collections.singletonList(taskId)))
            .thenReturn(Collections.singletonList(taskWithJurisdiction));

        final ArgumentCaptor<Iterable<DocumentTask>> captor = ArgumentCaptor.captor();
        updateDocumentTaskTasklet.execute(contribution, chunkContext);

        verify(documentTaskRepository).findAllByTaskStatus(TaskState.NEW.toString(), batchSize);
        verify(documentTaskRepository).findAllById(Collections.singletonList(taskId));
        verify(documentTaskRepository).saveAll(captor.capture());

        List<DocumentTask> savedTasks = new ArrayList<>();
        captor.getValue().forEach(savedTasks::add);

        assertEquals(1, savedTasks.size());
        DocumentTask savedTask = savedTasks.getFirst();
        assertNotNull(savedTask.getJurisdictionId());
        assertEquals("someJurisdiction", savedTask.getJurisdictionId());
        assertEquals(TaskState.NEW, savedTask.getTaskState());
    }
}