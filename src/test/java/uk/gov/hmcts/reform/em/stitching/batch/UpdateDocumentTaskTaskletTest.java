package uk.gov.hmcts.reform.em.stitching.batch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateDocumentTaskTaskletTest {

    private UpdateDocumentTaskTasklet updateDocumentTaskTasklet;

    @Mock
    private DocumentTaskRepository documentTaskRepository;

    @Mock
    private StepContribution contribution;

    @Mock
    private ChunkContext chunkContext;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void executeZeroRecords() {

        updateDocumentTaskTasklet = new UpdateDocumentTaskTasklet(documentTaskRepository, 0);
        when(documentTaskRepository.findAllByTaskStatus(anyString(), anyInt())).thenReturn(Collections.emptyList());

        updateDocumentTaskTasklet.execute(contribution, chunkContext);

        verify(documentTaskRepository, times(1)).findAllByTaskStatus(anyString(),anyInt());
        verify(documentTaskRepository, times(0)).findAllById(any());
        verify(documentTaskRepository, times(0)).saveAll(any());

    }

    @Test
    public void executeFiveRecords() {

        updateDocumentTaskTasklet = new UpdateDocumentTaskTasklet(documentTaskRepository, 5);
        when(documentTaskRepository.findAllByTaskStatus(anyString(), anyInt())).thenReturn(Arrays.asList(5L));
        when(documentTaskRepository.findAllById(any())).thenReturn(Arrays.asList(new DocumentTask()));

        updateDocumentTaskTasklet.execute(contribution, chunkContext);

        verify(documentTaskRepository, times(1)).findAllByTaskStatus(anyString(),anyInt());
        verify(documentTaskRepository, times(1)).findAllById(any());
        verify(documentTaskRepository, times(1)).saveAll(any());
    }
}
