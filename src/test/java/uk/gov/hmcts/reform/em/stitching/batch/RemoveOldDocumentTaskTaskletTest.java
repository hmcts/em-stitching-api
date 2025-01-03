package uk.gov.hmcts.reform.em.stitching.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoveOldDocumentTaskTaskletTest {

    private RemoveOldDocumentTaskTasklet removeOldDocumentTaskTasklet;

    @Mock
    private DocumentTaskRepository documentTaskRepository;

    @Mock
    private StepContribution contribution;

    @Mock
    private ChunkContext chunkContext;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void executeZeroRecords() {

        removeOldDocumentTaskTasklet = new RemoveOldDocumentTaskTasklet(documentTaskRepository, 0, 0);
        when(documentTaskRepository.findAllByCreatedDate(any(), anyInt())).thenReturn(Collections.emptyList());
        doNothing().when(documentTaskRepository).deleteAllById(any());

        removeOldDocumentTaskTasklet.execute(contribution, chunkContext);

        verify(documentTaskRepository, times(1)).findAllByCreatedDate(any(),anyInt());
        verify(documentTaskRepository, times(0)).deleteAllById(any());

    }

    @Test
    void executeTenRecords() {


        removeOldDocumentTaskTasklet = new RemoveOldDocumentTaskTasklet(documentTaskRepository, 0, 10);

        when(documentTaskRepository.findAllByCreatedDate(any(), anyInt())).thenReturn(createDocumentTaskIds());
        doNothing().when(documentTaskRepository).deleteAllById(any());

        removeOldDocumentTaskTasklet.execute(contribution, chunkContext);

        verify(documentTaskRepository, times(1)).findAllByCreatedDate(any(), anyInt());
        verify(documentTaskRepository, times(1)).deleteAllById(any());

    }

    private List<Long> createDocumentTaskIds() {
        return Arrays.asList(100L);
    }
}
