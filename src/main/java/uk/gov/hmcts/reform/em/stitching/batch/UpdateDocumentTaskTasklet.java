package uk.gov.hmcts.reform.em.stitching.batch;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;

import java.util.List;
import java.util.Objects;

public class UpdateDocumentTaskTasklet implements Tasklet {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDocumentTaskTasklet.class);

    private DocumentTaskRepository documentTaskRepository;

    int numberOfRows;

    public UpdateDocumentTaskTasklet(DocumentTaskRepository documentTaskRepository,
                                     int numberOfRows) {
        this.documentTaskRepository = documentTaskRepository;
        this.numberOfRows = numberOfRows;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        LOGGER.info("Update the DocumentTask status ");
        LOGGER.info("numberOfRows {}",numberOfRows);

        List<Long> documentTaskIds = documentTaskRepository.findAllByTaskStatus("NEW", numberOfRows);

        List<DocumentTask> documentTasks = documentTaskRepository.findAllById(documentTaskIds);

        LOGGER.info("documentTasks {}",documentTasks.size());

        if (CollectionUtils.isNotEmpty(documentTasks)) {
            LOGGER.info("Number of DocumentTask rows retrieved for updating was {}", documentTasks.size());
            documentTasks.forEach( documentTask -> {
                if (Objects.isNull(documentTask.getJurisdictionId())) {
                    documentTask.setTaskState(TaskState.FAILED);
                }
            });
            documentTaskRepository.saveAll(documentTasks);
        }

        return RepeatStatus.FINISHED;
    }

}
