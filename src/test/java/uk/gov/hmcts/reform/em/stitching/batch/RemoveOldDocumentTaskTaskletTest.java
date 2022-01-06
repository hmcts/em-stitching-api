package uk.gov.hmcts.reform.em.stitching.batch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class RemoveOldDocumentTaskTaskletTest {

    private RemoveOldDocumentTaskTasklet removeOldDocumentTaskTasklet;

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

        removeOldDocumentTaskTasklet = new RemoveOldDocumentTaskTasklet(documentTaskRepository, 0, 0);
        when(documentTaskRepository.findAllByCreatedDate(any())).thenReturn(createDocumentTaskIds());
        doNothing().when(documentTaskRepository).deleteById(any());

        removeOldDocumentTaskTasklet.execute(contribution, chunkContext);

        verify(documentTaskRepository, times(1)).findAllByCreatedDate(any());
        verify(documentTaskRepository, times(0)).deleteById(any());

    }

    @Test
    public void executeTenRecords() {


        removeOldDocumentTaskTasklet = new RemoveOldDocumentTaskTasklet(documentTaskRepository, 0, 10);

        when(documentTaskRepository.findAllByCreatedDate(any())).thenReturn(createDocumentTaskIds());
        doNothing().when(documentTaskRepository).deleteById(any());

        removeOldDocumentTaskTasklet.execute(contribution, chunkContext);

        verify(documentTaskRepository, times(1)).findAllByCreatedDate(any());
        verify(documentTaskRepository, times(1)).deleteById(any());

    }

    private List<Long> createDocumentTaskIds() {
        return Arrays.asList(100L);
    }
}
