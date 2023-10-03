package uk.gov.hmcts.reform.em.stitching.batch;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class RemoveOldDocumentTaskTasklet implements Tasklet {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveOldDocumentTaskTasklet.class);

    private DocumentTaskRepository documentTaskRepository;

    int numberOfDays;

    int numberOfRecords;

    public RemoveOldDocumentTaskTasklet(DocumentTaskRepository documentTaskRepository, int numberOfDays,
                                        int numberOfRecords) {
        this.documentTaskRepository = documentTaskRepository;
        this.numberOfDays = numberOfDays;
        this.numberOfRecords = numberOfRecords;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        Instant currentDate = Instant.now();
        Instant pastDate = currentDate.minus(numberOfDays, ChronoUnit.DAYS);

        LOGGER.info("Remove the DocumentTask before the {}", pastDate);

        List<Long> documentTaskIds = documentTaskRepository.findAllByCreatedDate(pastDate);

        LOGGER.info("Number of DocumentTask rows retrieved {}", documentTaskIds.size());

        if (CollectionUtils.isNotEmpty(documentTaskIds)) {
            documentTaskIds.stream()
                    .limit(numberOfRecords)
                    .forEach(documentTaskId -> documentTaskRepository.deleteById(documentTaskId));
        }
        LOGGER.info("Number of DocumentTask rows deleted {}", numberOfRecords);

        return RepeatStatus.FINISHED;
    }

}
