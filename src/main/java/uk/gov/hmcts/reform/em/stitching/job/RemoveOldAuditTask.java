package uk.gov.hmcts.reform.em.stitching.job;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.stitching.repository.EntityAuditEventRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class RemoveOldAuditTask implements Runnable {

    private static final String TASK_NAME = "remove-old-audit-tasks";
    private static final Logger logger = getLogger(RemoveOldAuditTask.class);

    private final EntityAuditEventRepository entityAuditEventRepository;

    @Value("${spring.batch.entityaudit.numberofdays}")
    private int numberOfDays;

    @Value("${spring.batch.entityaudit.numberofrecords}")
    private int numberOfRecords;

    public RemoveOldAuditTask(EntityAuditEventRepository entityAuditEventRepository) {
        this.entityAuditEventRepository = entityAuditEventRepository;
    }

    @Override
    public void run() {
        logger.info("Started {} job", TASK_NAME);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Instant currentDate = Instant.now();
            Instant pastDate = currentDate.minus(numberOfDays, ChronoUnit.DAYS);

            logger.info("Remove the EntityAuditEvent before the {}", pastDate);

            List<Long> auditEventIds = entityAuditEventRepository.findAllByModifiedDate(pastDate, numberOfRecords);

            if (CollectionUtils.isNotEmpty(auditEventIds)) {
                logger.info("Number of EntityAuditEvent rows retrieved: {}", auditEventIds.size());
                entityAuditEventRepository.deleteAllById(auditEventIds);
            } else {
                logger.info("No historical EntityAuditEvent records found requiring deletion.");
            }
        } catch (Exception e) {
            logger.error("Encountered error during {} task", TASK_NAME, e);
        }

        stopWatch.stop();
        logger.info("Finished {} job. Took {} ms", TASK_NAME, stopWatch.getDuration().toMillis());
    }
}