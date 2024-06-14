package uk.gov.hmcts.reform.em.stitching.config.audit;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uk.gov.hmcts.reform.em.stitching.domain.AbstractAuditingEntity;

public class EntityAuditEventListener extends AuditingEntityListener {

    private final Logger log = LoggerFactory.getLogger(EntityAuditEventListener.class);

    private static BeanFactory beanFactory;

    @PostPersist
    public void onPostCreate(AbstractAuditingEntity target) {
        try {
            AsyncEntityAuditEventWriter asyncEntityAuditEventWriter =
                    beanFactory.getBean(AsyncEntityAuditEventWriter.class);
            asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.CREATE);
        } catch (NoSuchBeanDefinitionException e) {
            log.error("No bean found for AsyncEntityAuditEventWriter");
        } catch (Exception e) {
            log.error("Exception while persisting create audit entity {}", e.toString());
        }
    }

    @PostUpdate
    public void onPostUpdate(AbstractAuditingEntity target) {
        try {
            AsyncEntityAuditEventWriter asyncEntityAuditEventWriter =
                    beanFactory.getBean(AsyncEntityAuditEventWriter.class);
            asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.UPDATE);
        } catch (NoSuchBeanDefinitionException e) {
            log.error("No bean found for AsyncEntityAuditEventWriter");
        } catch (Exception e) {
            log.error("Exception while persisting update audit entity {}", e.toString());
        }
    }

    @PostRemove
    public void onPostRemove(AbstractAuditingEntity target) {
        try {
            AsyncEntityAuditEventWriter asyncEntityAuditEventWriter =
                    beanFactory.getBean(AsyncEntityAuditEventWriter.class);
            asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.DELETE);
        } catch (NoSuchBeanDefinitionException e) {
            log.error("No bean found for AsyncEntityAuditEventWriter");
        } catch (Exception e) {
            log.error("Exception while persisting delete audit entity {}", e.toString());
        }
    }

    static void setBeanFactory(BeanFactory beanFactory) {
        EntityAuditEventListener.beanFactory = beanFactory;
    }

}
