package uk.gov.hmcts.reform.em.stitching.batch;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.domain.EntityAuditEvent;

@Service
@Transactional(propagation = Propagation.REQUIRED)
@SuppressWarnings("java:S899")
public class EntityValueProcessor implements ItemProcessor<EntityAuditEvent, EntityAuditEvent> {
    private final Logger log = LoggerFactory.getLogger(EntityValueProcessor.class);

    public EntityValueProcessor() {
    }

    @Override
    public EntityAuditEvent process(EntityAuditEvent entityAuditEvent) {
        entityAuditEvent.setEntityValueV2(entityAuditEvent.getEntityValue());
        entityAuditEvent.setEntityValueMigrated(true);
        return entityAuditEvent;
    }
}
