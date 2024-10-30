package uk.gov.hmcts.reform.em.stitching.batch;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;

@Component
public class StoreDocumentTaskRetryCount {
    private static final Logger log = LoggerFactory.getLogger(StoreDocumentTaskRetryCount.class);
    @PersistenceContext
    private final EntityManager entityManager;

    public StoreDocumentTaskRetryCount(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementRetryAttempts(DocumentTask documentTask) {
        log.debug("state saving,getRetryAttempts:{}", documentTask.getRetryAttempts());

        documentTask.setRetryAttempts(documentTask.getRetryAttempts() + 1);
        documentTask.setTaskState(TaskState.IN_PROGRESS);

        entityManager.merge(documentTask);

        entityManager.flush();
        log.debug("state saving DONE, getRetryAttempts:{}", documentTask.getRetryAttempts());

    }
}