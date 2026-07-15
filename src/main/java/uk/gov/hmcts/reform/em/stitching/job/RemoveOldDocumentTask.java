package uk.gov.hmcts.reform.em.stitching.job;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class RemoveOldDocumentTask implements Runnable {

    private static final String TASK_NAME = "remove-old-document-tasks";
    private static final Logger logger = getLogger(RemoveOldDocumentTask.class);

    private final DocumentTaskRepository documentTaskRepository;

    @Value("${spring.batch.documenttask.numberofdays}")
    private int numberOfDays;

    @Value("${spring.batch.documenttask.numberofrecords}")
    private int numberOfRecords;

    public RemoveOldDocumentTask(DocumentTaskRepository documentTaskRepository) {
        this.documentTaskRepository = documentTaskRepository;
    }

    @Override
    public void run() {
        logger.info("Started {} job", TASK_NAME);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Instant currentDate = Instant.now();
            Instant pastDate = currentDate.minus(numberOfDays, ChronoUnit.DAYS);

            logger.info("Remove the DocumentTask before the {}", pastDate);

            List<Long> documentTaskIds = documentTaskRepository.findAllByCreatedDate(pastDate, numberOfRecords);

            if (CollectionUtils.isNotEmpty(documentTaskIds)) {
                logger.info("Number of DocumentTask rows retrieved: {}", documentTaskIds.size());
                documentTaskRepository.deleteAllById(documentTaskIds);
            } else {
                logger.info("No historical DocumentTask records found requiring deletion.");
            }
        } catch (Exception e) {
            logger.error("Encountered error during {} task", TASK_NAME, e);
        }

        stopWatch.stop();
        logger.info("Finished {} job. Took {} ms", TASK_NAME, stopWatch.getDuration().toMillis());
    }
}